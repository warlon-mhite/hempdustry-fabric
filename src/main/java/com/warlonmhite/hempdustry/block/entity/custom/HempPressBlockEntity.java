package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.custom.HempPressBlock;
import com.warlonmhite.hempdustry.block.entity.ImplementedInventory;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.NamedMachineBlockEntity;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.screen.custom.HempPressScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
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
public class HempPressBlockEntity extends NamedMachineBlockEntity
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
    /** Throttles {@link #pushOutput}. Not persisted — a few ticks of timer is not worth a save field. */
    private int pushCooldown;

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

        boolean pressing = heated && canPress(world);
        if (pressing) {
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

        if (pushOutput(world, pos, state)) {
            dirty = true;
        }

        BlockState shown = state.with(HempPressBlock.LIT, heated).with(HempPressBlock.PRESSING, pressing);
        if (shown != state) {
            state = shown;
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

    /**
     * Passes one item of the output to whatever is behind the press, over the pour lip on its back.
     * Returns whether one went.
     *
     * <p><b>Pushed, because the heat claimed the extraction face.</b> A hopper pulls only from the
     * block above it, through that block's down face — and the block under the press is its heat.
     * So, like the Infuser, the press pushes instead of being pulled from.
     *
     * <p><b>Out of the back</b>, the face opposite the grate. Not the front, where a chest would hide
     * the heat the grate shows; and not a side, where presses standing in a row would pour into each
     * other, each pressing its neighbour's output. A row stands side by side with its chests behind
     * it. A press set <em>behind</em> another takes what the first one pours as input, so filtered
     * kief or bubble hash goes to rosin unattended — a layout somebody chose, never an accident.
     *
     * <p><b>One item every {@link InfuserBlockEntity#PUSH_COOLDOWN} ticks</b>, which is what a hopper
     * under a furnace takes: the press empties exactly as fast as it would if the heat were not in
     * the way, and a pour is never split between a neighbour and the slot. The neighbour is found the
     * Infuser's two ways, in its order — the Transfer API for pipes and storage-only blocks, then
     * {@code getInventoryAt} for an inventory minecart — and the Infuser's {@code pushOutput} says why
     * each is there.
     *
     * <p><b>With nothing behind it the output stays put.</b> A Dropper with no container in front
     * throws its item out, but a Dropper fires on a pulse; a press runs all day, and one worked by
     * hand would strew rosin over its own floor.
     */
    private boolean pushOutput(World world, BlockPos pos, BlockState state) {
        ItemStack output = getStack(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return false;
        }
        if (pushCooldown > 0) {
            pushCooldown--;
            return false;
        }
        pushCooldown = InfuserBlockEntity.PUSH_COOLDOWN;

        // The neighbour's face against the press's back points the way the press does.
        Direction facing = state.get(HempPressBlock.FACING);
        BlockPos behind = pos.offset(facing.getOpposite());

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, behind, facing);
        if (storage != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                if (storage.insert(ItemVariant.of(output), 1, transaction) == 1) {
                    transaction.commit();
                    removeStack(OUTPUT_SLOT, 1);
                    return true;
                }
            }
            // A real neighbour with no room. Keep the hopper cadence: it may have room in 8 ticks.
            return false;
        }

        Inventory target = HopperBlockEntity.getInventoryAt(world, behind);
        if (target == null) {
            pushCooldown = InfuserBlockEntity.PUSH_IDLE_COOLDOWN;
            return false;
        }
        // transfer() only fills the destination, so it is handed a copy and the press takes its own.
        if (HopperBlockEntity.transfer(this, target, output.copyWithCount(1), facing).isEmpty()) {
            removeStack(OUTPUT_SLOT, 1);
            return true;
        }
        return false;
    }

    public boolean isHeated() {
        return heated;
    }

    // ----- client animation -----

    /** Client only: the world time the current run of squeezes began, or -1 while idle. */
    private long strokeStart = -1;

    /**
     * How many ticks into the current squeeze the press is, for the renderer, or -1 while idle.
     *
     * <p>The client never sees {@code progress}, and syncing it would be a packet a tick. It does not
     * need to: a run of squeezes starts on the tick {@link HempPressBlock#PRESSING} turns on, and each
     * squeeze lasts exactly {@link #pressTime()} — progress resets to 0 and climbs again on the next
     * tick — so the phase falls out of the world clock.
     */
    public float strokeTicks(float tickDelta) {
        // ponytail: the phase restarts from the top whenever the press is first seen, or resumes
        // after its output filled up with progress half-decayed. It only draws a platen; sync progress
        // in the update packet if a stroke ever has to land on the exact tick the item comes out.
        if (world == null || !getCachedState().get(HempPressBlock.PRESSING)) {
            strokeStart = -1;
            return -1;
        }
        if (strokeStart < 0) {
            strokeStart = world.getTime();
        }
        return (world.getTime() - strokeStart) % pressTime() + tickDelta;
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

    // Furnace-shaped hopper access: in from above or the sides, out through the bottom. The bottom
    // is where the heat sits, though, so automation gets the output from pushOutput instead.
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
    protected Text getContainerName() {
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
