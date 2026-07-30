package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.custom.DecarboxylatorBlock;
import com.warlonmhite.hempdustry.block.entity.ImplementedInventory;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
public class DecarboxylatorBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos>, ImplementedInventory {

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

    /** Decarboxylated hemp yielded per bud. Buds are the good stuff and pay out accordingly. */
    public static final int BUDS_OUTPUT = 4;
    /** Decarboxylated hemp yielded per fan leaf — bulk trim, worth a quarter of a bud. */
    public static final int LEAF_OUTPUT = 1;

    // PropertyDelegate indices. COOK_TIME is a constant both sides know, so it isn't synced.
    public static final int PROPERTY_BURN_TIME = 0;
    public static final int PROPERTY_FUEL_TIME = 1;
    /** Per-tray cook progress occupies indices 2..4. */
    public static final int PROPERTY_FIRST_PROGRESS = 2;
    public static final int PROPERTY_COUNT = 5;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);

    /** Ticks of fuel left in the fire. */
    private int burnTime;
    /** Total burn time the current fuel item was worth, so the flame icon can be drawn as a fraction. */
    private int fuelTime;
    private final int[] progress = new int[TRAY_COUNT];

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_BURN_TIME -> burnTime;
                case PROPERTY_FUEL_TIME -> fuelTime;
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
        public int size() {
            return PROPERTY_COUNT;
        }
    };

    public DecarboxylatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECARBOXYLATOR, pos, state);
    }

    // ----- what the trays accept -----

    /**
     * How much decarboxylated hemp one of {@code stack}'s items is worth, or 0 if the Decarboxylator
     * won't take it. Both strains' buds count — writing this indica-only is exactly the kind of
     * accidental strain-specificity CLAUDE.md keeps a list of.
     */
    public static int outputPerItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.INDICA_BUDS || item == ModItems.SATIVA_BUDS) {
            return BUDS_OUTPUT;
        }
        if (item == ModItems.HEMP_LEAF) {
            return LEAF_OUTPUT;
        }
        return 0;
    }

    public static boolean isTrayInput(ItemStack stack) {
        return outputPerItem(stack) > 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return AbstractFurnaceBlockEntity.canUseAsFuel(stack);
    }

    /**
     * Burn time of a fuel item, via the same map vanilla furnaces use — so the mod's own
     * {@code FuelRegistry} entries (hemp stem, bales, the armour set) work here for free.
     */
    private static int burnTimeOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Map<Item, Integer> fuelTimes = AbstractFurnaceBlockEntity.createFuelTimeMap();
        return fuelTimes.getOrDefault(stack.getItem(), 0);
    }

    // ----- ticking -----

    public void tick(World world, BlockPos pos, BlockState state) {
        boolean wasBurning = isBurning();
        boolean dirty = false;

        if (isBurning()) {
            burnTime--;
        }

        boolean anyTrayReady = false;
        for (int tray = 0; tray < TRAY_COUNT; tray++) {
            if (canCook(tray)) {
                anyTrayReady = true;
                break;
            }
        }

        // Light the fire only when there is actually something to cook, so fuel is never wasted on
        // an idle machine (same contract as a furnace).
        if (!isBurning() && anyTrayReady) {
            ItemStack fuel = getStack(FUEL_SLOT);
            int time = burnTimeOf(fuel);
            if (time > 0) {
                burnTime = time;
                fuelTime = time;
                Item before = fuel.getItem();
                fuel.decrement(1);
                if (fuel.isEmpty()) {
                    // Buckets and the like leave their empty container behind, as in a furnace.
                    setStack(FUEL_SLOT, before.getRecipeRemainder() == null
                            ? ItemStack.EMPTY
                            : new ItemStack(before.getRecipeRemainder()));
                }
                dirty = true;
            }
        }

        for (int tray = 0; tray < TRAY_COUNT; tray++) {
            if (isBurning() && canCook(tray)) {
                progress[tray]++;
                if (progress[tray] >= COOK_TIME) {
                    progress[tray] = 0;
                    cook(tray);
                }
                dirty = true;
            } else if (progress[tray] > 0) {
                // Cools off rather than freezing, the way a furnace loses progress without fuel.
                progress[tray] = Math.max(0, progress[tray] - 2);
                dirty = true;
            }
        }

        if (wasBurning != isBurning()) {
            state = state.with(DecarboxylatorBlock.LIT, isBurning());
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            dirty = true;
        }

        if (dirty) {
            markDirty(world, pos, state);
        }
    }

    /** Whether this tray has valid input and somewhere for its output to go. */
    private boolean canCook(int tray) {
        ItemStack input = getStack(FIRST_TRAY_SLOT + tray);
        int per = outputPerItem(input);
        if (per == 0) {
            return false;
        }
        ItemStack output = getStack(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return per <= ModItems.DECARBOXYLATED_HEMP.getMaxCount();
        }
        return output.isOf(ModItems.DECARBOXYLATED_HEMP)
                && output.getCount() + per <= output.getMaxCount();
    }

    private void cook(int tray) {
        ItemStack input = getStack(FIRST_TRAY_SLOT + tray);
        int per = outputPerItem(input);
        if (per == 0) {
            return;
        }
        ItemStack output = getStack(OUTPUT_SLOT);
        if (output.isEmpty()) {
            setStack(OUTPUT_SLOT, new ItemStack(ModItems.DECARBOXYLATED_HEMP, per));
        } else {
            output.increment(per);
        }
        input.decrement(1);
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    // ----- inventory -----

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> isFuel(stack);
            case OUTPUT_SLOT -> false;
            default -> isTrayInput(stack);
        };
    }

    // Hopper access mirrors a furnace: input from above, fuel from the sides, output out the bottom.
    @Override
    public int[] getAvailableSlots(Direction side) {
        return switch (side) {
            case UP -> new int[]{FIRST_TRAY_SLOT, FIRST_TRAY_SLOT + 1, FIRST_TRAY_SLOT + 2};
            case DOWN -> new int[]{OUTPUT_SLOT, FUEL_SLOT};
            default -> new int[]{FUEL_SLOT};
        };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        // The output is always drainable. The fuel slot only gives back leftover containers
        // (an empty bucket), never the fuel a hopper just inserted.
        if (slot == OUTPUT_SLOT) {
            return true;
        }
        return slot == FUEL_SLOT && !isFuel(stack);
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
        return Text.translatable("block.hempdustry.decarboxylator");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new DecarboxylatorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    // ----- persistence -----

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putInt("BurnTime", burnTime);
        nbt.putInt("FuelTime", fuelTime);
        nbt.putIntArray("Progress", progress.clone());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        inventory.clear();
        Inventories.readNbt(nbt, inventory, registryLookup);
        burnTime = nbt.getInt("BurnTime");
        fuelTime = nbt.getInt("FuelTime");
        int[] saved = nbt.getIntArray("Progress");
        for (int tray = 0; tray < TRAY_COUNT; tray++) {
            progress[tray] = tray < saved.length ? saved[tray] : 0;
        }
    }
}
