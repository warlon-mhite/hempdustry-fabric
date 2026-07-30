package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.custom.InfuserBlock;
import com.warlonmhite.hempdustry.block.entity.ImplementedInventory;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreenHandler;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Infuser: a hempcrete tub built around a cauldron that simmers decarboxylated hemp into a
 * bucket of milk until you have cannabutter. Second and last stage of the cannabutter chain.
 *
 * <p><b>Heat comes from below, not from a fuel slot.</b> Put it over a campfire, a magma block or
 * any lit furnace and it simmers; take the heat away and progress <em>pauses</em> where it is rather
 * than resetting. Vanilla checks a neighbouring block continuously in plenty of places — hoppers,
 * powered rails, note blocks, farmland hydration — so this is a familiar idiom, and it means the
 * machine has a real footprint in the world instead of being a box you feed coal into.
 *
 * <p><b>The output slot is a preview.</b> From {@link #MIN_TIME} it shows the cannabutter you would
 * get if you pulled right now; nothing is consumed until you actually take it. That is what lets the
 * doc's two promises hold at once — a batch can be rushed early for a worse grade, and hemp added
 * mid-simmer still counts, because the tally is read at the moment of collection rather than banked
 * when the timer starts. Take it early and you get {@link Quality#ROUGH}; wait for
 * {@link #FULL_TIME} and the preview upgrades itself in place.
 *
 * <p>Hoppers deliberately cannot pull the preview before {@link #FULL_TIME}, so automation is always
 * the patient path and never quietly produces the worst grade behind your back.
 */
public class InfuserBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos>, ImplementedInventory {

    public static final int MILK_SLOT = 0;
    public static final int HEMP_SLOT = 1;
    public static final int WASHED_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int SLOT_COUNT = 4;

    /**
     * Earliest a batch can be taken at all, at {@link Quality#ROUGH}. Six in-game hours — the mod
     * reuses Minecraft's own hour (1000 ticks) rather than inventing a ratio, so "cannabutter takes
     * hours" translates literally.
     */
    public static final int MIN_TIME = 6000;
    /**
     * A finished simmer: eighteen in-game hours, the midpoint of the real 12–24 hour range. There is
     * no benefit to leaving it longer — it just waits, like a grown crop.
     */
    public static final int FULL_TIME = 18000;

    /** Hemp items one batch will draw on. Extra beyond this simply stays in the slots. */
    public static final int BATCH_CAP = 24;

    public static final int PROPERTY_PROGRESS = 0;
    public static final int PROPERTY_HEATED = 1;
    public static final int PROPERTY_COUNT = 2;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);

    private int progress;
    private boolean heated;
    /**
     * Last value a comparator would have read. Progress changes every tick but its 0–15 projection
     * only changes about fifteen times a batch, so comparators are only poked when the number they
     * would report actually moves — updating them every tick would be twenty needless neighbour
     * updates a second.
     */
    private int lastComparatorLevel;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_PROGRESS -> progress;
                case PROPERTY_HEATED -> heated ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case PROPERTY_PROGRESS -> progress = value;
                case PROPERTY_HEATED -> heated = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int size() {
            return PROPERTY_COUNT;
        }
    };

    public InfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFUSER, pos, state);
    }

    // ----- what the slots accept -----

    public static boolean isMilk(ItemStack stack) {
        return stack.isIn(ModTags.Items.MILK_BUCKETS);
    }

    public static boolean isUnwashedHemp(ItemStack stack) {
        return stack.isOf(ModItems.DECARBOXYLATED_HEMP);
    }

    public static boolean isWashedHemp(ItemStack stack) {
        return stack.isOf(ModItems.WASHED_DECARBOXYLATED_HEMP);
    }

    /**
     * Whether the block underneath is currently providing heat. Anything in
     * {@link ModTags.Blocks#HEAT_SOURCES} counts, and where that block carries a {@code LIT}
     * property it has to actually be lit — which is what makes a campfire (permanently lit, cheap)
     * a very different proposition from a furnace (only lit while it is itself busy smelting).
     */
    public static boolean isHeatedFrom(BlockState below) {
        if (!below.isIn(ModTags.Blocks.HEAT_SOURCES)) {
            return false;
        }
        return !below.contains(Properties.LIT) || below.get(Properties.LIT);
    }

    // ----- ticking -----

    public void tick(World world, BlockPos pos, BlockState state) {
        boolean wasHeated = heated;
        heated = isHeatedFrom(world.getBlockState(pos.down()));

        boolean dirty = false;

        if (heated && hasIngredients()) {
            if (progress < FULL_TIME) {
                progress++;
                dirty = true;
            }
        } else if (!hasIngredients() && progress != 0) {
            // Losing the milk or all the hemp abandons the batch outright; losing only the heat
            // leaves progress where it was, so a campfire going out is a pause and not a disaster.
            progress = 0;
            dirty = true;
        }

        // The preview is rebuilt every tick rather than only at the thresholds, so adding hemp
        // mid-simmer is reflected immediately and the player can see the batch getting stronger.
        if (refreshPreview()) {
            dirty = true;
        }

        if (wasHeated != heated) {
            state = state.with(InfuserBlock.HEATED, heated);
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            dirty = true;
        }

        int level = getComparatorOutput();
        if (level != lastComparatorLevel) {
            lastComparatorLevel = level;
            world.updateComparators(pos, state.getBlock());
            dirty = true;
        }

        if (dirty) {
            markDirty(world, pos, state);
        }
    }

    private boolean hasIngredients() {
        return isMilk(getStack(MILK_SLOT)) && totalHemp() > 0;
    }

    /** Hemp the batch will actually draw on, both slots together, capped. */
    private int totalHemp() {
        return Math.min(BATCH_CAP, unwashedInBatch() + washedInBatch());
    }

    private int unwashedInBatch() {
        ItemStack stack = getStack(HEMP_SLOT);
        return isUnwashedHemp(stack) ? stack.getCount() : 0;
    }

    private int washedInBatch() {
        ItemStack stack = getStack(WASHED_SLOT);
        return isWashedHemp(stack) ? stack.getCount() : 0;
    }

    /**
     * Splits the capped batch across the two hemp types, spending unwashed first so a mixed batch
     * keeps as much of its washed hemp as possible in the tally — which is what decides whether the
     * grade can reach {@link Quality#CLEAN} or {@link Quality#PERFECT}.
     */
    private int[] batchSplit() {
        int unwashed = unwashedInBatch();
        int washed = washedInBatch();
        int total = unwashed + washed;
        if (total <= BATCH_CAP) {
            return new int[]{unwashed, washed};
        }
        int takeUnwashed = Math.min(unwashed, BATCH_CAP);
        return new int[]{takeUnwashed, BATCH_CAP - takeUnwashed};
    }

    public boolean isReady() {
        return progress >= MIN_TIME;
    }

    public boolean isFullySimmered() {
        return progress >= FULL_TIME;
    }

    /** The cannabutter this batch would yield right now, or empty if it isn't ready. */
    private ItemStack previewStack() {
        if (!isReady() || !hasIngredients()) {
            return ItemStack.EMPTY;
        }
        int[] split = batchSplit();
        ItemStack butter = new ItemStack(ModItems.CANNABUTTER);
        butter.set(ModComponents.STRENGTH, split[0] + split[1]);
        butter.set(ModComponents.QUALITY, Quality.of(isFullySimmered(), split[1], split[0]));
        return butter;
    }

    /** Keeps the output slot showing the current preview. Returns whether anything changed. */
    private boolean refreshPreview() {
        ItemStack wanted = previewStack();
        ItemStack shown = getStack(OUTPUT_SLOT);
        if (ItemStack.areEqual(wanted, shown)) {
            return false;
        }
        setStack(OUTPUT_SLOT, wanted);
        return true;
    }

    /**
     * Commits the batch: called when the player actually takes the preview out of the output slot.
     * Spends the hemp, turns the milk bucket into an empty one and starts the timer over.
     */
    public void onPreviewTaken() {
        int[] split = batchSplit();
        getStack(HEMP_SLOT).decrement(split[0]);
        getStack(WASHED_SLOT).decrement(split[1]);

        ItemStack milk = getStack(MILK_SLOT);
        milk.decrement(1);
        if (milk.isEmpty()) {
            // Hand the bucket back, the way a brewing stand leaves you the glass bottle. Vanilla's
            // recipe-remainder mechanism only fires for real crafting recipes, never for a block
            // entity, so this has to be done by hand.
            setStack(MILK_SLOT, new ItemStack(Items.BUCKET));
        }

        progress = 0;
        markDirty();
    }

    public int getProgress() {
        return progress;
    }

    public boolean isHeated() {
        return heated;
    }

    /**
     * What a comparator behind this block reads. <b>Batch progress, not how full the container is</b>
     * — 0 idle, 1–14 climbing through the simmer, and <b>15 only once the batch is fully
     * simmered</b>.
     *
     * <p>Fill level would be useless here: the output slot holds one item whether the batch is a
     * rushed Rough or a finished Perfect, so a stock container comparator reads the same either way
     * and cannot tell a player anything they want to know. Reporting state instead of fullness is
     * well-trodden vanilla ground — cauldrons report water level, composters their fill stage, cake
     * its bites, beehives their honey, respawn anchors their charge.
     *
     * <p>What it buys: a lamp or note block that fires the moment a batch finishes, or redstone that
     * gates something else on it. (Extraction itself needs no redstone — hoppers already refuse an
     * unfinished batch, see {@link #canExtract}.)
     */
    public int getComparatorOutput() {
        if (progress <= 0) {
            return 0;
        }
        if (isFullySimmered()) {
            return 15;
        }
        return 1 + Math.min(13, progress * 13 / FULL_TIME);
    }

    // ----- inventory -----

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return switch (slot) {
            case MILK_SLOT -> isMilk(stack);
            case HEMP_SLOT -> isUnwashedHemp(stack);
            case WASHED_SLOT -> isWashedHemp(stack);
            default -> false;
        };
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return switch (side) {
            case UP -> new int[]{MILK_SLOT, HEMP_SLOT, WASHED_SLOT};
            case DOWN -> new int[]{OUTPUT_SLOT, MILK_SLOT};
            default -> new int[]{MILK_SLOT, HEMP_SLOT, WASHED_SLOT};
        };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        if (slot == OUTPUT_SLOT) {
            // Automation only ever gets a finished batch. Without this a hopper would snatch the
            // Rough preview the instant it appeared, making an automated Infuser strictly worse
            // than a hand-tended one — the opposite of what automation should buy you.
            return isFullySimmered();
        }
        // The emptied bucket can be drawn off; a full one the player just inserted cannot.
        return slot == MILK_SLOT && stack.isOf(Items.BUCKET);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.world != null
                && this.world.getBlockEntity(this.pos) == this
                && player.squaredDistanceTo(Vec3d.ofCenter(this.pos)) <= 64.0D;
    }

    // ----- screen -----

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.hempdustry.infuser");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new InfuserScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    // ----- persistence -----

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putInt("Progress", progress);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        inventory.clear();
        Inventories.readNbt(nbt, inventory, registryLookup);
        progress = nbt.getInt("Progress");
    }
}
