package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.custom.HempPressBlock;
import com.warlonmhite.hempdustry.block.entity.ImplementedInventory;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.screen.custom.HempPressScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Hemp Press's brain: heat and pressure, one slot in and one slot out.
 *
 * <h2>What it is for</h2>
 *
 * Rosin. Squeeze hash between two hot plates and the trichome heads burst; what runs out is a
 * solventless concentrate at the same tier butane would reach, <b>without modelling the one step in
 * cannabis production that actually hurts people</b>. Home butane "blasting" explodes and burns
 * people and is criminalised as a process in places where the plant is legal; a press gets to the
 * same place with a lever and a campfire. See {@code concentrates.md} §0.
 *
 * <p>It also breaks and scutches retted stems, which is the job {@code materials.md} has wanted off
 * the crafting grid since 2026-08-22 — and the reason the block serves more than one role, which is
 * what a new block is supposed to do.
 *
 * <h2>Heated from below, and it invents nothing</h2>
 *
 * {@link InfuserBlockEntity#isHeatedFrom(BlockState)} already exists, already handles the {@code LIT}
 * check, and already reads {@code pos.down()}. This calls it. That is right on the chemistry too:
 * hash rosin is pressed at 160–180 °F, which is <em>low</em>, so a campfire under a press is
 * period-correct and thermally correct at once.
 *
 * <p><b>No quality axis, deliberately.</b> The temperature ladder in {@code #hempdustry:heat_sources}
 * is real and the trade argues about it constantly — hotter yields more and tastes worse — but rosin
 * has nothing for that to land on. Until it does, the ladder is read as a boolean here exactly as
 * the Infuser reads it. See {@code concentrates.md} §8.
 */
public class HempPressBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos>, ImplementedInventory {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    /**
     * Ticks one squeeze takes — <b>a furnace's exact 200</b>.
     *
     * <p>Vanilla's cook ladder, read out of the 1.21.11 jar rather than recalled: blast furnace and
     * smoker <b>100</b>, furnace <b>200</b>, campfire <b>600</b>. The Decarboxylator's 500 sits near
     * the campfire because it is justified as a low, slow oven and gets its throughput from three
     * parallel trays instead. A press is not slow — it is a mechanical squeeze — so it belongs at the
     * furnace's number, and a batch of {@code SiftingBoxBlock.YIELD} pieces costs 90 seconds,
     * exactly what smelting nine iron costs.
     */
    public static final int PRESS_TIME = 200;

    /** How much rosin one piece of filtered hashish yields. Pressing hash is a 60–90% step in life. */
    public static final int ROSIN_OUTPUT = 1;
    // There is deliberately no constant for kief -> hashish or bubble hash -> filtered hashish:
    // both are 1:1, because pressing changes a powder's SHAPE and not its chemistry. A number would
    // imply there was something to tune.
    /**
     * Fibre off one retted stem in the press, against {@code ModRecipeProvider.FIBER_PER_RETTED_STEM}
     * on the crafting grid.
     *
     * <p>Higher rather than exclusive: the grid recipe stays, because taking it away is a balance
     * change to shipped content that has nothing to do with concentrates. The press is simply the
     * better way to do it once you have one.
     */
    public static final int PRESSED_FIBER_OUTPUT = 8;

    public static final int PROPERTY_PROGRESS = 0;
    public static final int PROPERTY_PRESS_TIME = 1;
    public static final int PROPERTY_HEATED = 2;
    public static final int PROPERTY_COUNT = 3;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);

    private int progress;
    /** Recomputed every tick from the block below; synced so the screen's flame is truthful. */
    private boolean heated;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_PROGRESS -> progress;
                case PROPERTY_PRESS_TIME -> pressTime();
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

    public HempPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEMP_PRESS, pos, state);
    }

    /** {@link #PRESS_TIME} after {@code world.machineSpeedMultiplier}, synced so the bar cannot lie. */
    public static int pressTime() {
        double speed = HempdustryConfig.get().world().machineSpeedMultiplier();
        return Math.max(1, (int) Math.round(PRESS_TIME / speed));
    }

    /**
     * What one of {@code stack}'s items presses into, or empty if the press will not take it.
     *
     * <p>Through the synchronized recipe view so it answers on a client too — the screen's input
     * slot asks the same question, and shift-click routing has to agree with the server's without a
     * round trip.
     */
    public static ItemStack resultFor(@Nullable World world, ItemStack stack) {
        if (world == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        SingleStackRecipeInput input = new SingleStackRecipeInput(stack);
        return world.getRecipeManager().getSynchronizedRecipes()
                .getFirstMatch(ModRecipes.PRESSING_TYPE, input, world)
                .map(entry -> entry.value().craft(input, world.getRegistryManager()))
                .orElse(ItemStack.EMPTY);
    }

    public static boolean isInput(@Nullable World world, ItemStack stack) {
        return !resultFor(world, stack).isEmpty();
    }

    // ----- ticking -----

    public void tick(World world, BlockPos pos, BlockState state) {
        boolean wasHeated = heated;
        heated = InfuserBlockEntity.isHeatedFrom(world.getBlockState(pos.down()));
        boolean dirty = wasHeated != heated;

        if (heated && canPress(world)) {
            progress++;
            if (progress >= pressTime()) {
                progress = 0;
                press(world);
            }
            dirty = true;
        } else if (progress > 0) {
            // Cools off rather than freezing, the way a furnace loses progress when its fire goes out.
            progress = Math.max(0, progress - 2);
            dirty = true;
        }

        if (state.get(HempPressBlock.LIT) != heated) {
            state = state.with(HempPressBlock.LIT, heated);
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            dirty = true;
        }

        if (dirty) {
            markDirty(world, pos, state);
        }
    }

    /** Whether there is something to press and somewhere for it to go. */
    private boolean canPress(World world) {
        ItemStack result = resultFor(world, getStack(INPUT_SLOT));
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = getStack(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return result.getCount() <= result.getMaxCount();
        }
        return ItemStack.areItemsAndComponentsEqual(output, result)
                && output.getCount() + result.getCount() <= output.getMaxCount();
    }

    private void press(World world) {
        ItemStack input = getStack(INPUT_SLOT);
        ItemStack result = resultFor(world, input);
        if (result.isEmpty()) {
            return;
        }
        ItemStack output = getStack(OUTPUT_SLOT);
        if (output.isEmpty()) {
            setStack(OUTPUT_SLOT, result);
        } else {
            output.increment(result.getCount());
        }
        input.decrement(1);
    }

    public boolean isHeated() {
        return heated;
    }

    // ----- inventory -----

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && isInput(this.world, stack);
    }

    // Furnace-shaped hopper access: in from above or the sides, out through the bottom.
    @Override
    public int[] getAvailableSlots(Direction side) {
        return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{INPUT_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
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
        return Text.translatable("block.hempdustry.hemp_press");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new HempPressScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    // ----- persistence -----

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, inventory);
        view.putInt("Progress", progress);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        inventory.clear();
        Inventories.readData(view, inventory);
        progress = view.getInt("Progress", 0);
        // `heated` is deliberately not persisted: it is a fact about the block below, and the first
        // tick after load reads it. Saving it would let a press come back hot over a cold campfire.
    }
}
