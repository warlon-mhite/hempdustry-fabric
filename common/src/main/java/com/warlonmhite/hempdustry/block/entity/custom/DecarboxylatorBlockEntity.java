package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.custom.DecarboxylatorBlock;
import com.warlonmhite.hempdustry.block.entity.ImplementedInventory;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.NamedMachineBlockEntity;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreenHandler;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The Decarboxylator's brain: an oven that gently heats hemp until it is decarboxylated — the real
 * step that has to happen before cannabinoids will dissolve into fat, and the reason you can't just
 * stir raw buds into butter.
 *
 * <p><b>Three independent trays, one shared fire, one shared output.</b> Each tray cooks whatever
 * it holds on its own timer, so a tray of buds and a tray of leaf finish independently and nothing
 * waits on a batch. Fuel is furnace-style — it burns down while <em>anything</em> is cooking, which
 * means running all three trays is three times as fuel-efficient as running one. That's deliberate:
 * the machine rewards being filled.
 *
 * <p>Output does <b>not</b> convert in place the way a brewing stand does. It can't: buds yield
 * {@value #BUDS_OUTPUT} decarboxylated hemp each, so a tray of 64 would need far more room than the
 * tray has. Everything lands in the single collection slot instead, which also gives the player one
 * place to pull from and one slot for a hopper to drain.
 *
 * <p>Both strains' buds and the strain-agnostic {@code hemp_leaf} all produce the same
 * {@code decarboxylated_hemp}: strain identity is carried by the smoking system, not by the edibles
 * chain, so the pipeline downstream of here stays a single line of items.
 */
public class DecarboxylatorBlockEntity extends NamedMachineBlockEntity
        implements MenuProvider, ImplementedInventory {

    public static final int FUEL_SLOT = 0;
    /** The three tray slots are contiguous from here. */
    public static final int FIRST_TRAY_SLOT = 1;
    public static final int TRAY_COUNT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int SLOT_COUNT = 5;

    /**
     * Ticks one item spends in a tray. Sits between ore smelting (200t) and campfire smoking
     * (600t) — this is a slow, low oven, not a furnace, and the three parallel trays are where the
     * throughput comes from rather than raw speed.
     */
    public static final int COOK_TIME = 500;

    /**
     * The cook time this oven is actually running, after {@code world.machineSpeedMultiplier}.
     *
     * <p>Synced through the property delegate below rather than read from the client's own config,
     * so the progress arrows stay truthful on a server whose settings differ from the player's file.
     */
    public static int cookTime() {
        double speed = HempdustryConfig.get().world().machineSpeedMultiplier();
        return Math.max(1, (int) Math.round(COOK_TIME / speed));
    }

    /**
     * Decarboxylated hemp yielded per bud. Buds are the good stuff and pay out accordingly.
     *
     * <p><b>These two are the shipped recipes' numbers, not the machine's.</b> Since the conversion
     * became {@code hempdustry:decarboxylating} they are read by {@code ModRecipeProvider} and by
     * nothing at runtime — a datapack decides what any given tray-load is worth. Kept as constants
     * so the balance is still written down in one place.
     */
    public static final int BUDS_OUTPUT = 4;
    /** Decarboxylated hemp yielded per fan leaf — bulk trim, worth a quarter of a bud. */
    public static final int LEAF_OUTPUT = 1;
    /**
     * Decarboxylated hemp yielded per schwag: half a bud. Still a bud, so still heat-activated, but a
     * spoiled one costs its grower half its oven value on top of its whole high.
     */
    public static final int SCHWAG_OUTPUT = BUDS_OUTPUT / 2;
    // There is deliberately NO hashish figure here. Nothing in the hash family decarboxylates:
    // the oven takes plant matter, the bowl takes resin. See ModRecipeProvider and SiftingBoxBlock.

    // ContainerData indices. The cook time is synced because the config can change it.
    public static final int PROPERTY_BURN_TIME = 0;
    public static final int PROPERTY_FUEL_TIME = 1;
    /** Per-tray cook progress occupies indices 2..4. */
    public static final int PROPERTY_FIRST_PROGRESS = 2;
    public static final int PROPERTY_COOK_TIME = 5;
    public static final int PROPERTY_COUNT = 6;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    /** Ticks of fuel left in the fire. */
    private int burnTime;
    /** Total burn time the current fuel item was worth, so the flame icon can be drawn as a fraction. */
    private int fuelTime;
    private final int[] progress = new int[TRAY_COUNT];

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_BURN_TIME -> burnTime;
                case PROPERTY_FUEL_TIME -> fuelTime;
                case PROPERTY_COOK_TIME -> cookTime();
                default -> {
                    int tray = index - PROPERTY_FIRST_PROGRESS;
                    yield tray >= 0 && tray < TRAY_COUNT ? progress[tray] : 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case PROPERTY_BURN_TIME -> burnTime = value;
                case PROPERTY_FUEL_TIME -> fuelTime = value;
                default -> {
                    int tray = index - PROPERTY_FIRST_PROGRESS;
                    if (tray >= 0 && tray < TRAY_COUNT) {
                        progress[tray] = value;
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return PROPERTY_COUNT;
        }
    };

    public DecarboxylatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECARBOXYLATOR, pos, state);
    }

    // ----- what the trays accept -----

    /**
     * What one of {@code stack}'s items cooks into, or {@link ItemStack#EMPTY} if the Decarboxylator
     * won't take it.
     *
     * <p><b>This used to compare item identity against three fields</b>, which is exactly where a
     * third-party strain's buds hit a wall: everything upstream — the strain registry, the tags, the
     * smoking gear — would take them, and the oven would not. It is now a lookup in
     * {@code hempdustry:decarboxylating}, so another mod adds a recipe and is done.
     *
     * <p>A {@code null} world (a block entity that has not been placed into one yet) answers empty
     * rather than throwing: there are no recipes to consult, so there is no input either.
     */
    public static ItemStack resultFor(@Nullable Level world, ItemStack stack) {
        if (world == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        // Through the synchronized view, so this answers on a client too — the screen's tray slots
        // ask the same question. See ModRecipes#allOfType.
        return Services.RECIPES.firstMatch(world, ModRecipes.DECARBOXYLATING_TYPE, input)
                .map(entry -> entry.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    public static boolean isTrayInput(@Nullable Level world, ItemStack stack) {
        return !resultFor(world, stack).isEmpty();
    }

    public static boolean isFuel(@Nullable Level world, ItemStack stack) {
        return world != null && world.fuelValues().isFuel(stack);
    }

    /**
     * Burn time of a fuel item, via the same map vanilla furnaces use — so the mod's own
     * {@code FuelValues} entries (hemp stem, bales, the armour set) work here for free.
     */
    private static int burnTimeOf(@Nullable Level world, ItemStack stack) {
        if (world == null || stack.isEmpty()) {
            return 0;
        }
        return world.fuelValues().burnDuration(stack);
    }

    // ----- ticking -----

    public void tick(Level world, BlockPos pos, BlockState state) {
        boolean wasBurning = isBurning();
        boolean dirty = false;

        if (isBurning()) {
            burnTime--;
        }

        boolean anyTrayReady = false;
        for (int tray = 0; tray < TRAY_COUNT; tray++) {
            if (canCook(world, tray)) {
                anyTrayReady = true;
                break;
            }
        }

        // Light the fire only when there is actually something to cook, so fuel is never wasted on
        // an idle machine (same contract as a furnace).
        if (!isBurning() && anyTrayReady) {
            ItemStack fuel = getItem(FUEL_SLOT);
            int time = burnTimeOf(world, fuel);
            if (time > 0) {
                burnTime = time;
                fuelTime = time;
                Item before = fuel.getItem();
                fuel.shrink(1);
                if (fuel.isEmpty()) {
                    // Buckets and the like leave their empty container behind, as in a furnace.
                    // getCraftingRemainder is a template since 26.1, null when there is none.
                    ItemStackTemplate remainder = before.getCraftingRemainder();
                    setItem(FUEL_SLOT, remainder != null ? remainder.create() : ItemStack.EMPTY);
                }
                dirty = true;
            }
        }

        for (int tray = 0; tray < TRAY_COUNT; tray++) {
            if (isBurning() && canCook(world, tray)) {
                progress[tray]++;
                if (progress[tray] >= cookTime()) {
                    progress[tray] = 0;
                    cook(world, tray);
                }
                dirty = true;
            } else if (progress[tray] > 0) {
                // Cools off rather than freezing, the way a furnace loses progress without fuel.
                progress[tray] = Math.max(0, progress[tray] - 2);
                dirty = true;
            }
        }

        if (wasBurning != isBurning()) {
            state = state.setValue(DecarboxylatorBlock.LIT, isBurning());
            world.setBlock(pos, state, Block.UPDATE_ALL);
            dirty = true;
        }

        if (dirty) {
            setChanged(world, pos, state);
        }
    }

    /**
     * Whether this tray has valid input and somewhere for its output to go.
     *
     * <p>The collection slot is shared, so a tray whose recipe yields something the slot is not
     * already holding simply waits — which is the same stall a furnace has when its output slot holds
     * the wrong item, and the reason the components have to match and not just the item.
     */
    private boolean canCook(Level world, int tray) {
        ItemStack result = resultFor(world, getItem(FIRST_TRAY_SLOT + tray));
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return result.getCount() <= result.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void cook(Level world, int tray) {
        ItemStack input = getItem(FIRST_TRAY_SLOT + tray);
        ItemStack result = resultFor(world, input);
        if (result.isEmpty()) {
            return;
        }
        ItemStack output = getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            setItem(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }
        input.shrink(1);
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    // ----- inventory -----

    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> isFuel(this.level, stack);
            case OUTPUT_SLOT -> false;
            default -> isTrayInput(this.level, stack);
        };
    }

    // Hopper access mirrors a furnace: input from above, fuel from the sides, output out the bottom.
    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case UP -> new int[]{FIRST_TRAY_SLOT, FIRST_TRAY_SLOT + 1, FIRST_TRAY_SLOT + 2};
            case DOWN -> new int[]{OUTPUT_SLOT, FUEL_SLOT};
            default -> new int[]{FUEL_SLOT};
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // The output is always drainable. The fuel slot only gives back leftover containers
        // (an empty bucket), never the fuel a hopper just inserted.
        if (slot == OUTPUT_SLOT) {
            return true;
        }
        return slot == FUEL_SLOT && !isFuel(this.level, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(Vec3.atCenterOf(this.worldPosition)) <= 64.0D;
    }

    // ----- screen -----

    @Override
    protected Component getContainerName() {
        return Component.translatable("block.hempdustry.decarboxylator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DecarboxylatorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    // ----- persistence -----

    // Since 1.21.9 a block entity is read and written through ValueInput/ValueOutput rather than a raw
    // CompoundTag. The keys are unchanged, so a world written by 2.0.0-beta on 1.21.1 still loads.
    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, inventory);
        view.putInt("BurnTime", burnTime);
        view.putInt("FuelTime", fuelTime);
        view.putIntArray("Progress", progress.clone());
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        inventory.clear();
        ContainerHelper.loadAllItems(view, inventory);
        burnTime = view.getIntOr("BurnTime", 0);
        fuelTime = view.getIntOr("FuelTime", 0);
        int[] saved = view.getIntArray("Progress").orElse(new int[0]);
        for (int tray = 0; tray < TRAY_COUNT; tray++) {
            progress[tray] = tray < saved.length ? saved[tray] : 0;
        }
    }
}
