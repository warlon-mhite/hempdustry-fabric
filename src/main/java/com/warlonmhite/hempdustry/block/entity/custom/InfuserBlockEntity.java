package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.api.HempdustryEvents;
import com.warlonmhite.hempdustry.block.custom.InfuserBlock;
import com.warlonmhite.hempdustry.block.entity.ImplementedInventory;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.recipe.InfusingRecipe;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreenHandler;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
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
 * <p><b>Ingredients are consumed as they enter the batch, not when the result is collected.</b> The
 * batch — {@link #haveMilk}, {@link #batchUnwashed}, {@link #batchWashed} — is bookkeeping on this
 * block entity rather than items sitting in slots. Two different rhythms:
 * <ul>
 *   <li><b>Milk is poured in by hand, one at a time</b> — right-click the tub with a bucket, or
 *       point a dispenser at it — and the empty comes straight back ({@link #fill}). It is not a
 *       slot: the tub holds one milk or none, and a two-state vessel is what vanilla fills in the
 *       world, like a cauldron. <b>One milk buys one cannabutter:</b> the tub stays full for the
 *       whole batch and takes the next bucket only once the result has been collected.</li>
 *   <li><b>Hemp dissolves gradually</b>, one item per {@link #ABSORB_INTERVAL}, and only until
 *       {@link #minTime()}. That is what locks a batch: past the loading window the absorber has had
 *       all its turns, so nothing more goes in however much room is left.</li>
 * </ul>
 *
 * <p>Draining the hemp by {@code minTime()} rather than over the whole simmer is load-bearing, not a
 * detail. <b>Strength is the whole batch, so the whole batch has to be paid for by the time the
 * result can first be taken.</b> Spread it over {@link #fullTime()} instead and an early pull leaves
 * a third-consumed batch, which can only be resolved by refunding the rest (full Strength for a
 * third of the hemp — an exploit), destroying it (the slots visibly empty for nothing), or scaling
 * Strength down (which makes rushing strictly dominated, and deletes it as a real choice).
 *
 * <p>What survives intact is both of the original promises. <b>Topping up still works</b> — hemp
 * added during the window is absorbed on the next interval — though late hemp genuinely cannot catch
 * up, which is the point. And <b>rushing still works</b>, because what the timer gates is the grade,
 * not the ingredients: from {@link #minTime()} the output slot previews what this batch would yield
 * right now — {@link Quality#ROUGH} for a poorly prepped batch, but already {@link Quality#STANDARD}
 * if every item was washed — and it upgrades in place as the grading score climbs.
 * Taking it ends the batch and starts the next.
 *
 * <p><b>Output automation is a spout, not a hopper below.</b> A hopper pulls from the inventory above
 * it through that inventory's <em>down</em> face, which is the block this machine reads its heat
 * from — so the two can never coexist and the tub pushes instead. See {@link #pushOutput}. A batch is
 * pushed once it has earned the best grade it can ({@link #isAtBestQuality}), which is <em>not</em>
 * the same as a full simmer: only an all-washed batch actually needs the full timer, because Perfect
 * is the one grade gated on it. Automation is therefore always the patient path — it never hands
 * over a grade worse than waiting would have produced — without wasting time it cannot spend.
 *
 * <p><b>The two hemp slots are interchangeable</b> — either accepts washed or unwashed. They are two
 * slots so that a batch can <em>mix</em> the two, which is what makes {@link Quality#CLEAN} reachable
 * at all; dedicating one slot per type would have meant a batch could never be half-and-half without
 * the player micromanaging which slot held what.
 */
public class InfuserBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos>, ImplementedInventory {

    /**
     * <b>Retired in 2.0.1.</b> Milk used to go in through a slot here and its empty come back out
     * through {@link #RETIRED_BUCKET_SLOT}; it is poured in by hand now ({@link #fill}). Both indices
     * stay reserved because a save records items by slot number — renumbering would load an old
     * world's hemp into the output slot. Nothing is ever put in either again, and
     * {@link #ejectRetiredSlots} hands back whatever an old world left there.
     */
    public static final int RETIRED_MILK_SLOT = 0;
    /** The two interchangeable hemp slots are contiguous from here. */
    public static final int FIRST_HEMP_SLOT = 1;
    public static final int HEMP_SLOT_COUNT = 2;
    public static final int OUTPUT_SLOT = 3;
    /** Retired in 2.0.1 with {@link #RETIRED_MILK_SLOT}: where emptied buckets used to come back. */
    public static final int RETIRED_BUCKET_SLOT = 4;
    public static final int SLOT_COUNT = 5;
    private static final int[] RETIRED_SLOTS = {RETIRED_MILK_SLOT, RETIRED_BUCKET_SLOT};

    /**
     * <b>Default</b> earliest a batch can be taken at all, and the zero point of {@link Quality}'s
     * time dial. Six in-game hours — the mod reuses Minecraft's own hour (1000 ticks) rather than
     * inventing a ratio, so "cannabutter takes hours" translates literally.
     *
     * <p><b>Nothing in this class may branch on this constant.</b> It is the seed for
     * {@code HempdustryConfig.Infuser.DEFAULT} and the number the prose below is written about;
     * every runtime decision goes through {@link #minTime()}, because a server can move it. Mix the
     * two and the machine only half-obeys its own config: the absorber runs on the configured window
     * while the preview waits on the hardcoded one, so a server that lowered the minimum gets a
     * batch that has spent its hemp and cannot be collected. (Being a compile-time constant, this is
     * inlined at every use site, so reading it from the config record loads no class — which matters,
     * because that record is built before anything else in {@code onInitialize}.)
     */
    public static final int MIN_TIME = 6000;

    /** The configured earliest pull, or {@link #MIN_TIME} when nothing has overridden it. */
    public static int minTime() {
        return HempdustryConfig.get().infuser().minTimeTicks();
    }
    /**
     * <b>Default</b> finished simmer: eighteen in-game hours, the midpoint of the real 12–24 hour
     * range. There is no benefit to leaving it longer — it just waits, like a grown crop. Same rule
     * as {@link #MIN_TIME}: read {@link #fullTime()}, never this.
     */
    public static final int FULL_TIME = 18000;

    /** The configured full simmer. Always greater than {@link #minTime()} — the config clamps it. */
    public static int fullTime() {
        return HempdustryConfig.get().infuser().fullTimeTicks();
    }

    /**
     * Most hemp one batch can hold. <b>Derived, not chosen:</b> it is exactly how many
     * {@link #ABSORB_INTERVAL}s fit in {@link #minTime()}, so the cap is a consequence of the
     * dissolve rate rather than an independent number that has to be justified on its own.
     */
    public static final int BATCH_CAP = 24;

    /**
     * Ticks between one hemp dissolving into the batch and the next — {@code minTime() / BATCH_CAP},
     * about 12.5 real seconds. Absorption runs only while the batch is simmering and stops dead at
     * {@link #minTime()}, which is what makes the batch lock itself: after that the absorber has had
     * all the turns it is going to get, so nothing more can go in whatever room is left.
     *
     * <p><b>Exactly one per interval, never catching up.</b> Backfilling from
     * {@code progress / ABSORB_INTERVAL} would let hemp dropped in at tick 5999 be absorbed 23 at a
     * time, which throws away the point: reaching a full-strength batch should require the hemp to
     * have been <em>present</em> for the whole loading window. Load halfway through and you can only
     * reach 12.
     */
    public static int absorbInterval() {
        return Math.max(1, minTime() / BATCH_CAP);
    }

    /** Ticks between attempts to pour a finished batch out of the spout. A hopper's own cadence. */
    public static final int PUSH_COOLDOWN = 8;

    /**
     * Longer wait before re-scanning when the spout found <b>nothing to pour into</b>.
     *
     * <p>A finished batch nobody has collected is the state an un-automated Infuser sits in
     * indefinitely, and every scan runs {@code HopperBlockEntity.getInventoryAt}, which falls back to
     * an entity query for inventory minecarts when the block in front isn't a container. Retrying
     * that two and a half times a second for ever, for a machine that is simply waiting to be
     * emptied by hand, is work with no possible outcome.
     *
     * <p>A second, not longer: this is also how long an automation build waits after a chest is
     * placed in front of an already-finished tub, and much past that reads as broken. The cadence for
     * a spout that <em>is</em> feeding something stays {@link #PUSH_COOLDOWN}, so throughput is
     * unchanged.
     */
    public static final int PUSH_IDLE_COOLDOWN = 20;

    public static final int PROPERTY_PROGRESS = 0;
    public static final int PROPERTY_HEATED = 1;
    /** Washed share of the batch, 0–100, or -1 when there is no batch to grade. */
    public static final int PROPERTY_WASHED_PERCENT = 2;
    /**
     * The two timings, synced.
     *
     * <p><b>This is what saves the config from needing a network packet.</b> The screen draws the
     * bar, the minimum mark and the next-grade mark from these, so it reads the numbers <em>this
     * block</em> is running on rather than whatever the client's own config file happens to say —
     * true for a vanilla-config client on a tuned server, and true again the moment
     * {@code /hempdustry reload} changes them mid-batch.
     */
    public static final int PROPERTY_MIN_TIME = 3;
    public static final int PROPERTY_FULL_TIME = 4;
    /**
     * Whether there is milk in the tub, for the screen's milk indicator. The model already shows it,
     * but the block is behind the screen while the screen is open.
     */
    public static final int PROPERTY_FILLED = 5;
    public static final int PROPERTY_COUNT = 6;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);

    private int progress;
    /** Whether something hot is underneath. Reported to the GUI's flame; not a blockstate on its own. */
    private boolean heated;
    /**
     * Whether there is milk in the tub. <b>One milk, one cannabutter</b> — this is a flag rather
     * than a counter on purpose: the tub takes a bucket only when it is empty, and stays full until
     * the batch is collected. Set by {@link #fill}, cleared by {@link #onPreviewTaken}.
     */
    private boolean haveMilk;
    /** Hemp already drawn into the running batch, by type. Together capped at {@link #BATCH_CAP}. */
    private int batchUnwashed;
    private int batchWashed;
    /**
     * Throttles {@link #pushOutput}. Not persisted — a few ticks of timer is not worth a save field.
     */
    private int pushCooldown;

    /**
     * Whether the output slot held a preview at the end of the previous tick. Drives
     * {@link #collectIfPreviewTaken()}, which is how a batch closes when something took the
     * cannabutter by a route this class never sees.
     *
     * <p><b>Persisted, and that is not fussiness.</b> Without it, a world saved in the window between
     * an extraction and the next tick would reload believing no preview had ever been shown, skip the
     * sweep, and hand out a second cannabutter for the same batch. One item, but a dupe is a dupe.
     * A missing key reads as {@code false}, which is exactly the old behaviour, so the field is safe
     * to have added.
     */
    private boolean previewShown;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_PROGRESS -> progress;
                case PROPERTY_HEATED -> heated ? 1 : 0;
                // Synced so the screen can work out when this batch's grade will next improve,
                // which under the score-based grading depends on the ratio and not just the clock.
                case PROPERTY_WASHED_PERCENT -> washedPercent();
                case PROPERTY_MIN_TIME -> minTime();
                case PROPERTY_FULL_TIME -> fullTime();
                case PROPERTY_FILLED -> haveMilk ? 1 : 0;
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

    /**
     * The conversion this world is running, or {@code null} if a pack has removed it. Everything
     * below reads the items it accepts and the item it yields from here rather than naming them, so
     * a pack can widen or rebalance the tub without a code change — see {@link InfusingRecipe}.
     *
     * <p><b>Cheap enough to call per tick:</b> it is a lookup in the recipe manager's by-type
     * multimap and allocates nothing. The shipped recipe still points {@code container} at
     * {@code #hempdustry:milk_buckets}, so widening what counts as milk stays a tag edit.
     */
    @Nullable
    private static InfusingRecipe recipe(@Nullable World world) {
        return world == null ? null : InfusingRecipe.of(world);
    }

    public static boolean isMilk(@Nullable World world, ItemStack stack) {
        InfusingRecipe recipe = recipe(world);
        return recipe != null && recipe.container().test(stack);
    }

    public static boolean isUnwashedHemp(@Nullable World world, ItemStack stack) {
        InfusingRecipe recipe = recipe(world);
        return recipe != null && recipe.hemp().test(stack);
    }

    public static boolean isWashedHemp(@Nullable World world, ItemStack stack) {
        InfusingRecipe recipe = recipe(world);
        return recipe != null && recipe.washedHemp().test(stack);
    }

    /** Either kind of decarboxylated hemp — both hemp slots accept both. */
    public static boolean isHemp(@Nullable World world, ItemStack stack) {
        InfusingRecipe recipe = recipe(world);
        return recipe != null && (recipe.hemp().test(stack) || recipe.washedHemp().test(stack));
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
        heated = isHeatedFrom(world.getBlockState(pos.down()));

        boolean dirty = false;

        // Before anything else touches the output slot: did something take the cannabutter without
        // going through removeStack? See collectIfPreviewTaken.
        if (collectIfPreviewTaken()) {
            dirty = true;
        }

        if (ejectRetiredSlots(world, pos)) {
            dirty = true;
        }

        if (canSimmer()) {
            // Losing the heat leaves progress where it is, so a campfire going out is a pause and
            // not a disaster. A batch that has run out of hemp entirely pauses the same way, and
            // resumes if more arrives before minTime().
            progress++;
            // Exactly one hemp per interval, and only during the loading window. See ABSORB_INTERVAL.
            if (progress <= minTime() && progress % absorbInterval() == 0) {
                absorbOne();
            }
            dirty = true;
        }

        // The preview is rebuilt every tick rather than only at the thresholds, so hemp absorbed
        // mid-simmer is reflected immediately and the player can see the batch getting stronger.
        if (refreshPreview()) {
            dirty = true;
        }

        if (pushOutput(world, pos, state)) {
            dirty = true;
        }

        BlockState wanted = state.with(InfuserBlock.FILLED, isFilled())
                .with(InfuserBlock.INFUSING, isInfusing());
        if (wanted != state) {
            state = wanted;
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            dirty = true;
        }

        // No explicit comparator update here, and no "only when the level changed" guard either.
        // BlockEntity#markDirty(World, BlockPos, BlockState) already ends in
        // world.updateComparators(pos, state.getBlock()) unconditionally (verified in the 1.21.1
        // jar), so any tick that marks this block entity dirty has poked the comparators anyway. The
        // guard that used to sit here therefore saved nothing and cost an extra updateComparators
        // plus a getComparatorOutput -- which runs isAtBestQuality -- on every tick it did fire.
        // Vanilla's furnace marks dirty every burning tick for exactly the same reason.
        // Last, so it records what the slot looks like once this tick has finished with it.
        previewShown = !getStack(OUTPUT_SLOT).isEmpty();

        if (dirty) {
            markDirty(world, pos, state);
        }
    }

    /**
     * Closes the batch out when the preview left the slot by a route that never called
     * {@link #removeStack}. Returns whether it fired.
     *
     * <h2>There are three ways out of this slot, not two</h2>
     *
     * A {@code Slot} take is one, and a hopper's {@code Inventory#removeStack} is the second — both
     * are already handled. The third is the <b>Fabric Transfer API</b>, which every Fabric-side pipe,
     * cable and storage mod uses, and which reaches this inventory through a fallback Fabric
     * registers automatically for any {@code Inventory} block entity. Its
     * {@code InventorySlotWrapper} extends {@code SingleStackStorage} and moves items with
     * {@code getStack}/{@code setStack} — <b>it never calls {@code removeStack}</b>. So the batch was
     * never closed, {@link #refreshPreview()} handed out a fresh cannabutter on the next tick, and a
     * single batch produced them for ever.
     *
     * <h2>Why this is a tick-boundary sweep and not a setStack hook</h2>
     *
     * The obvious fix — notice the write in {@code setStack} — is wrong, and expensively so. Transfer
     * API extraction is <b>transactional</b>: a pipe routinely opens a transaction, takes an item to
     * see whether it fits somewhere, and <b>aborts</b>, which restores the stack. A {@code setStack}
     * hook would close the batch on that speculative take and the abort would not undo it, quietly
     * destroying live batches on any build that merely probes this machine.
     *
     * <p>Observing the slot at a tick boundary is immune to that, because a transaction is opened and
     * resolved inside one call stack — by the time this runs, the take has either committed or been
     * rolled back, and the slot tells the truth either way. It also covers every future extraction
     * route without needing to know about it.
     *
     * <p>{@link #previewShown} rather than "is ready and has a batch but the slot is empty": the
     * stateless version looks equivalent but breaks on {@code /hempdustry reload}, where lowering
     * {@code minTimeTicks} can make a running batch newly {@link #isReady()} while the slot is still
     * legitimately empty — and the sweep would then destroy it.
     */
    private boolean collectIfPreviewTaken() {
        if (!previewShown || !getStack(OUTPUT_SLOT).isEmpty() || !hasBatch()) {
            return false;
        }
        onPreviewTaken();
        return true;
    }

    /**
     * Pours one milk into the tub, if the tub is empty and {@code milk} counts as milk. Returns
     * whether it went in. Deliberately independent of heat and of hemp: milk in the tub is what
     * makes {@link #isFilled()} true, nothing else.
     *
     * <p><b>Filled in the world, not through a slot.</b> The tub holds one milk or none, and a
     * two-state vessel is what vanilla fills by hand — a water bucket on a cauldron. The slot this
     * replaced emptied a bucket by itself and parked the empty in a second slot: two slots of GUI to
     * say one bit. {@link InfuserBlock#onUseWithItem} is the click and {@code ModDispenserBehaviors}
     * the dispenser; both come through here.
     *
     * <p><b>One milk, one cannabutter.</b> {@link #haveMilk} gates this, so a full tub refuses and
     * the caller keeps its bucket.
     *
     * <p>Only the milk is taken. The container is the caller's to hand back, because only the caller
     * knows where it goes — a player's hand or a dispenser's slots — and {@link #emptiedContainer}
     * says what it is. The sound and the game event are vanilla's cauldron fill.
     */
    public boolean fill(ItemStack milk, @Nullable Entity actor) {
        if (haveMilk || this.world == null || this.world.isClient() || !isMilk(this.world, milk)) {
            return false;
        }
        haveMilk = true;
        // Now rather than on the next tick, so a second click a moment later already sees a full tub.
        this.world.setBlockState(this.pos, getCachedState().with(InfuserBlock.FILLED, true), Block.NOTIFY_ALL);
        this.world.playSound(null, this.pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        this.world.emitGameEvent(actor, GameEvent.FLUID_PLACE, this.pos);
        markDirty();
        return true;
    }

    /**
     * Hands back whatever a world from before 2.0.1 left in the two retired slots, by popping it out
     * of the top of the tub. Returns whether anything came out.
     *
     * <p>Neither slot is in the screen or on any face now, so a bucket left in one would otherwise be
     * unreachable until the tub was broken. Out of the top because the underside is the heat and the
     * spout pours cannabutter only. {@link ItemScatterer} rather than {@code Block.dropStack}, which
     * obeys {@code doTileDrops}: these are the player's own items, not the block's drops.
     */
    private boolean ejectRetiredSlots(World world, BlockPos pos) {
        boolean ejected = false;
        for (int slot : RETIRED_SLOTS) {
            ItemStack stack = getStack(slot);
            if (!stack.isEmpty()) {
                setStack(slot, ItemStack.EMPTY);
                ItemScatterer.spawn(world, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack);
                ejected = true;
            }
        }
        return ejected;
    }

    /**
     * What to hand back when a milk is poured in — <b>the item's own recipe remainder</b>, and a
     * plain bucket only if it hasn't got one.
     *
     * <p>Vanilla's remainder mechanism fires for real crafting recipes and never for a block, so the
     * empty has to be handed back by hand; the question is only what "the empty" is. Reading
     * it off the item rather than assuming a bucket is what makes
     * {@code #hempdustry:milk_buckets} safe to widen. The tag folds in {@code #c:buckets/milk}, so
     * <b>the milk that arrives here may belong to a mod this one has never heard of</b> — and if it
     * came in a bottle or a gourd, minting a bucket for it would be free iron, while a hardcoded
     * bucket would also quietly destroy a container worth more than one. Both of vanilla's and this
     * mod's milks answer {@code BUCKET} here, so nothing changes for either.
     */
    public static ItemStack emptiedContainer(ItemStack milk) {
        // getRecipeRemainder is an ItemStack since 1.21.2 — empty, not null, when there is none.
        ItemStack remainder = milk.getItem().getRecipeRemainder();
        return remainder.isEmpty() ? new ItemStack(Items.BUCKET) : remainder.copy();
    }

    /**
     * Whether a batch can make progress this tick: hot, with milk in the tub, and with hemp to work
     * on — either already dissolved in or still in the slots.
     *
     * <p>The hemp clause is why a batch that has run out of hemp <em>pauses</em> rather than
     * finishing empty: without it a tub emptied at tick 100 would ride the clock to
     * {@link #minTime()} with nothing in it, and the milk would be stranded on a batch that can never
     * produce a preview. Pausing also stops the absorption clock, so the window can't be run down
     * while there is nothing to put in it.
     *
     * <p><b>The {@link #isAtBestQuality} clause stops the clock the moment further simmering would
     * achieve nothing</b>, which keeps three things honest at once: the tub stops bubbling when the
     * batch is done rather than churning away at a finished result, the progress bar lands exactly on
     * full instead of overshooting a scale it has already left behind, and {@code progress} never
     * records time that meant anything. Safe from oscillating because the washed ratio is frozen
     * after {@link #minTime()}, so once true this can never go back to false within a batch.
     */
    private boolean canSimmer() {
        return heated
                && haveMilk
                && (batchHemp() > 0 || availableHemp() > 0)
                && progress < fullTime()
                && !isAtBestQuality();
    }

    /** Dissolves a single hemp out of the slots into the batch. Unwashed first, so a mixed batch
     * keeps as much washed hemp in the tally as it can — that is what decides whether the grade can
     * reach {@link Quality#CLEAN} or {@link Quality#PERFECT}. */
    private void absorbOne() {
        if (batchHemp() >= BATCH_CAP) {
            return;
        }
        for (int pass = 0; pass < 2; pass++) {
            boolean wantWashed = pass == 1;
            for (int i = 0; i < HEMP_SLOT_COUNT; i++) {
                ItemStack stack = getStack(FIRST_HEMP_SLOT + i);
                if (wantWashed ? isWashedHemp(this.world, stack) : isUnwashedHemp(this.world, stack)) {
                    stack.decrement(1);
                    if (wantWashed) {
                        batchWashed++;
                    } else {
                        batchUnwashed++;
                    }
                    return;
                }
            }
        }
    }

    /** Hemp sitting in the slots that the batch could still draw on. */
    private int availableHemp() {
        int total = 0;
        for (int i = 0; i < HEMP_SLOT_COUNT; i++) {
            ItemStack stack = getStack(FIRST_HEMP_SLOT + i);
            if (isHemp(this.world, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Hemp already committed to the running batch. */
    private int batchHemp() {
        return batchUnwashed + batchWashed;
    }

    /**
     * The purity dial: what share of the batch's hemp was washed, 0–100, or {@code -1} when there is
     * no batch to grade. Integer-floored on purpose — {@link Quality} treats 100 as "not one
     * unwashed item went in", so a single unwashed among a thousand has to read as 99.
     */
    public int washedPercent() {
        int total = batchHemp();
        return total == 0 ? -1 : batchWashed * 100 / total;
    }

    /**
     * The patience dial: how far through the <em>collectable</em> window the batch is, 0–100.
     * Measured from {@link #minTime()} rather than from zero, because nothing before that can be
     * graded at all — that is where the scale has to start for the score to mean anything.
     *
     * <p>Origin and span both come off the config. Taking the origin from the hardcoded default
     * while dividing by the configured span puts the whole dial out of register on any server that
     * moved the timings, and since this dial is half of {@link Quality#of}, that hands players the
     * <em>wrong grade</em> rather than merely the wrong bar.
     */
    public int timePercent() {
        int span = fullTime() - minTime();
        return MathHelper.clamp((progress - minTime()) * 100 / span, 0, 100);
    }

    private boolean hasBatch() {
        return haveMilk && batchHemp() > 0;
    }

    /**
     * Whether the tub holds liquid. This drives {@link InfuserBlock#FILLED} and therefore the
     * block's texture, and is deliberately independent of heat: what makes the tub look full is milk
     * being in it, not something burning underneath. True from the moment milk is poured in and false
     * again the moment the batch is collected — a direct readout of {@link #haveMilk} with nothing
     * else mixed in.
     */
    public boolean isFilled() {
        return haveMilk;
    }

    /**
     * Whether a batch is actually simmering right now. Drives {@link InfuserBlock#INFUSING} and so
     * the bubbling and steam — which must not play for a heated but empty tub, and stop once the
     * batch is finished, so the ambience means "something is happening in here" and nothing else.
     */
    public boolean isInfusing() {
        return canSimmer();
    }

    /** Whether there is anything in the output slot to look at yet. */
    public boolean isReady() {
        return progress >= minTime();
    }

    /**
     * The best grade this batch will ever reach — what it would earn at a full simmer.
     *
     * <p>This is knowable mid-simmer only because <b>the washed ratio is frozen after
     * {@link #minTime()}</b>: absorption stops there, so nothing can change {@code washedPercent}
     * afterwards and the only dial still moving is time. If hemp could still be absorbed later this
     * would be a guess, and everything built on it below would be wrong.
     */
    public Quality bestQuality() {
        return Quality.of(100, washedPercent());
    }

    /**
     * Whether the batch has already earned the best grade it can, so that further simmering is
     * <b>time spent for nothing</b>.
     *
     * <p>Only an all-washed batch actually needs the full timer, because Perfect is the one grade
     * gated on it. Everything else peaks earlier — a half-washed batch tops out at Standard at 67% of
     * the cook, and a batch with a single unwashed item in it reaches Clean at 74% and will never
     * improve. This is the mod's single definition of "done", shared by the spout, the comparator and
     * the hopper guard, so those three can never disagree about it.
     */
    public boolean isAtBestQuality() {
        return isReady() && hasBatch() && Quality.of(timePercent(), washedPercent()) == bestQuality();
    }

    /**
     * The cannabutter this batch would yield right now. Only called when one is actually wanted.
     *
     * <p>The item and its count come from the recipe; the two components do not. Strength and
     * Quality are what <em>this machine</em> measured — how much hemp dissolved in, and the score
     * over the simmer and the washed ratio — so they are stamped on whatever the recipe yields
     * rather than being part of it.
     */
    private ItemStack previewStack(InfusingRecipe recipe, int strength, Quality quality) {
        ItemStack butter = recipe.result().copy();
        butter.set(ModComponents.STRENGTH, strength);
        butter.set(ModComponents.QUALITY, quality);
        return butter;
    }

    /** Whether {@code shown} is already the preview a batch of this strength and grade wants. */
    private static boolean showsPreview(ItemStack shown, InfusingRecipe recipe, int strength, Quality quality) {
        return shown.isOf(recipe.result().getItem())
                && shown.getCount() == recipe.result().getCount()
                && Integer.valueOf(strength).equals(shown.get(ModComponents.STRENGTH))
                && shown.get(ModComponents.QUALITY) == quality;
    }

    /**
     * Pours a finished batch out of the spout into whatever is against that face. Returns whether
     * anything moved.
     *
     * <p><b>Two ways in, tried in that order.</b> The Fabric Transfer API first — that is the only
     * thing an AE2 interface, a Modern Industrialization pipe or any other storage-only neighbour
     * answers to, and until 2026-08-23 the spout could not see any of them (roadmap D16). Fabric
     * registers an automatic fallback for every {@code Inventory} block entity, so the same lookup
     * covers chests, hoppers, barrels and droppers too; it is the general path rather than a special
     * case. The old {@code Inventory} path stays behind it for the one thing a <em>block</em> lookup
     * structurally cannot find: an <b>inventory minecart</b> parked against the spout, which is an
     * entity.
     *
     * <p>One deliberate behaviour change came with it. {@code HopperBlockEntity.transfer} sets a
     * receiving hopper's transfer cooldown to 8 ticks; inserting through a {@code Storage} does not,
     * so a hopper against the spout now passes the cannabutter along up to eight ticks sooner. It is
     * a timing difference and not a duplication — the spout's own cadence is unchanged.
     *
     * <p><b>This exists because heat-from-below claimed the extraction face.</b> A hopper pulls from
     * the inventory above it through that inventory's <em>down</em> face — the same block this
     * machine reads its heat from. Campfire below, no hopper; hopper below, no heat. So the tub
     * pushes instead of being pulled from, which is not how any vanilla processing block behaves;
     * the visible spout on {@link InfuserBlock#FACING} is what stops that from being a rule nobody
     * could guess.
     *
     * <p><b>Pushed once the batch is at its best grade</b> ({@link #isAtBestQuality}), not once the
     * timer runs out. Those differ for every batch that is not all-washed: a half-washed one tops out
     * at Standard at 67% of the cook and a batch with a single unwashed item reaches Clean at 74%,
     * and simmering either of them longer changes nothing at all. Waiting anyway would have been pure
     * dead time. Automation is still always the patient path — it never hands over a grade worse than
     * waiting would have produced — it just does not wait for nothing.
     *
     * <p>Taking a batch <em>early</em>, at a grade below its best, is deliberately still not
     * automatable: that trade is only meaningful once cannabutter <em>does</em> something, and it
     * would need a GUI toggle to express. See the deferred-toggle note in CLAUDE.md §3.
     *
     * <p>Pushing routes through {@link #removeStack}, so it closes the batch out exactly as a player
     * or a hopper taking it would — the one place that guarantee lives.
     */
    private boolean pushOutput(World world, BlockPos pos, BlockState state) {
        if (!isAtBestQuality() || getStack(OUTPUT_SLOT).isEmpty() || !state.contains(InfuserBlock.FACING)) {
            return false;
        }
        // A finished batch with nothing to pour into would otherwise re-scan every tick, and the
        // miss path is the expensive one: a BlockApiLookup miss followed by getInventoryAt, which
        // runs an entity query for inventory minecarts. Vanilla hoppers throttle the same work to
        // 8 ticks; so does this, and it backs off to PUSH_IDLE_COOLDOWN when the last scan found no
        // target of either kind.
        if (pushCooldown > 0) {
            pushCooldown--;
            return false;
        }
        pushCooldown = PUSH_COOLDOWN;

        Direction facing = state.get(InfuserBlock.FACING);
        BlockPos spoutPos = pos.offset(facing);
        // The receiving side is the face of the target that we are pouring into.
        Direction receivingSide = facing.getOpposite();
        // transfer() and insert() both only ever touch the destination — neither removes from the
        // source — so this passes a copy and does the removal itself.
        ItemStack pending = getStack(OUTPUT_SLOT).copy();

        // The Transfer API first, because that is the only thing an AE2 interface, a Modern
        // Industrialization pipe or any other storage-only neighbour answers to. Fabric's automatic
        // fallback means this also finds every ordinary Inventory, so it is the general path and not
        // a special case.
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, spoutPos, receivingSide);
        if (storage != null) {
            if (!insertWhole(storage, pending)) {
                // A real target that happens to be full. Keep the hopper cadence rather than backing
                // off — it may have room again in eight ticks.
                return false;
            }
            removeStack(OUTPUT_SLOT);
            return true;
        }

        // Nothing exposes a Storage here, so this is the last thing left that can still be a
        // container: an inventory minecart parked against the spout, which is an entity and so is
        // invisible to a block lookup. getInventoryAt is the only thing that finds one.
        Inventory target = HopperBlockEntity.getInventoryAt(world, spoutPos);
        if (target == null) {
            pushCooldown = PUSH_IDLE_COOLDOWN;
            return false;
        }
        int before = pending.getCount();
        ItemStack leftover = HopperBlockEntity.transfer(this, target, pending, receivingSide);
        if (leftover.getCount() >= before) {
            return false;
        }
        // The preview is always a single item, so anything moving means all of it moved. The count
        // check above rather than isEmpty() keeps that from becoming a silent dupe if it ever isn't.
        removeStack(OUTPUT_SLOT);
        return true;
    }

    /**
     * Inserts {@code stack} into {@code storage}, <b>all of it or none of it</b>. Returns whether it
     * went.
     *
     * <p>The all-or-nothing rule is the same invariant the {@code Inventory} path's count check
     * keeps, and it matters for the same reason: the batch is closed out by removing the whole
     * preview, so a partial insert would hand a neighbour one item and destroy the rest. A
     * transaction makes that free — a partial result is simply never committed, and closing without
     * committing rolls it back.
     */
    private static boolean insertWhole(Storage<ItemVariant> storage, ItemStack stack) {
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            if (moved < stack.getCount()) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    /**
     * Keeps the output slot showing the current preview. Returns whether anything changed.
     *
     * <p><b>The comparison is made on the two numbers, not on a freshly built stack.</b> This runs
     * every tick for the whole life of a batch — and keeps running, for ever, for a finished batch
     * nobody has collected, which is the state an un-automated Infuser spends most of its time in.
     * Building a candidate {@link ItemStack} to compare against (two component writes, each copying
     * the component map) meant twenty throwaway stacks a second per machine to answer a question
     * that is two integer comparisons. Strength and grade between them move about fifteen times a
     * batch; that is how often a stack is now allocated.
     */
    private boolean refreshPreview() {
        ItemStack shown = getStack(OUTPUT_SLOT);
        InfusingRecipe recipe = recipe(this.world);
        if (recipe == null || !isReady() || !hasBatch()) {
            if (shown.isEmpty()) {
                return false;
            }
            setStack(OUTPUT_SLOT, ItemStack.EMPTY);
            return true;
        }
        int strength = batchHemp();
        Quality quality = Quality.of(timePercent(), washedPercent());
        if (showsPreview(shown, recipe, strength, quality)) {
            return false;
        }
        setStack(OUTPUT_SLOT, previewStack(recipe, strength, quality));
        return true;
    }

    /**
     * Closes the batch out: called when the player actually takes the preview from the output slot.
     * The ingredients were already spent as they were absorbed, so all this does is clear the batch
     * and start the timer over.
     *
     * <p>Clearing {@link #haveMilk} here is what makes the tub take milk again — the emptying of the
     * tub and the taking of the cannabutter are the same event, which is the whole "one milk, one
     * cannabutter" rule.
     */
    public void onPreviewTaken() {
        // Fired before the numbers are wiped, because this is the last moment they exist — and only
        // when there was actually a batch to close, which is what stops a second listener call when
        // both the slot hook and removeStack fire for one collection (the close-out itself is
        // idempotent, an event is not).
        int strength = batchHemp();
        if (strength > 0 && world != null) {
            HempdustryEvents.AFTER_INFUSE.invoker().onInfused(
                    world, pos, strength, Quality.of(timePercent(), washedPercent()));
        }
        haveMilk = false;
        batchUnwashed = 0;
        batchWashed = 0;
        progress = 0;
        markDirty();
    }

    /**
     * Any route out of the output slot closes the batch out — <b>including a hopper</b>, which is the
     * whole reason this is overridden here rather than left to the screen handler's
     * {@code PreviewSlot#onTakeItem}. A hopper pulls through {@code Inventory#removeStack} and never
     * touches a {@code Slot}, so without this it would take the cannabutter, leave the batch running,
     * and have a fresh preview handed to it on the next tick — an unlimited supply from one batch.
     */
    @Override
    public ItemStack removeStack(int slot, int count) {
        ItemStack taken = ImplementedInventory.super.removeStack(slot, count);
        if (slot == OUTPUT_SLOT && !taken.isEmpty()) {
            onPreviewTaken();
        }
        return taken;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack taken = ImplementedInventory.super.removeStack(slot);
        if (slot == OUTPUT_SLOT && !taken.isEmpty()) {
            onPreviewTaken();
        }
        return taken;
    }

    /**
     * Throws away an uncollected preview. Called just before the block spills its contents, because
     * the preview is not a real item yet: without this, breaking a tub at {@link #minTime()} would
     * drop the cannabutter <em>and</em> refund every hemp that went into it, which is free
     * cannabutter on repeat. Spilling a batch returns the ingredients, never the product.
     */
    public void discardPreview() {
        setStack(OUTPUT_SLOT, ItemStack.EMPTY);
    }

    /**
     * The hemp currently committed to a batch, as items, so the block can spill it when broken.
     * Without this, breaking a simmering Infuser would silently destroy up to {@link #BATCH_CAP}
     * hemp — it has already left the slots that {@code ItemScatterer} walks.
     *
     * <p><b>The milk in the tub is deliberately not returned.</b> Its bucket came back the moment it
     * was poured in, so handing a full milk bucket back as well would mint a bucket out of nothing —
     * three iron a go. Breaking the tub spills the milk and you keep the empty, which is both
     * dupe-free and the physically obvious outcome. At most one milk is ever at stake, since the tub
     * holds one at a time.
     */
    public DefaultedList<ItemStack> getBatchItems() {
        DefaultedList<ItemStack> spill = DefaultedList.of();
        InfusingRecipe recipe = recipe(this.world);
        if (recipe == null) {
            // No conversion means nothing was ever absorbed under one; nothing to hand back.
            return spill;
        }
        // The batch counts items, not stacks, so which of an ingredient's matches went in is not
        // recorded anywhere. The first match is the only answer available, and the right one for
        // every single-item ingredient — which both of the shipped ones are.
        addBatchStacks(spill, InfusingRecipe.representative(recipe.hemp()).getItem(), batchUnwashed);
        addBatchStacks(spill, InfusingRecipe.representative(recipe.washedHemp()).getItem(), batchWashed);
        return spill;
    }

    private static void addBatchStacks(DefaultedList<ItemStack> out, Item item, int count) {
        if (item == Items.AIR) {
            return; // an ingredient with no matching stacks: nothing sensible to hand back
        }
        int max = item.getMaxCount();
        while (count > 0) {
            int take = Math.min(count, max);
            out.add(new ItemStack(item, take));
            count -= take;
        }
    }

    public int getProgress() {
        return progress;
    }

    public boolean isHeated() {
        return heated;
    }

    /**
     * What a comparator behind this block reads. <b>Batch progress, not how full the container is</b>
     * — 0 idle, 1–14 climbing through the simmer, and <b>15 once the batch has reached the best
     * grade it can</b>, which is the same moment the spout pours it and the same moment a hopper
     * would be allowed to take it. One definition of "done" across all three, so a redstone signal
     * can never disagree with what the machine actually does.
     *
     * <p>Note that for most batches 15 arrives <em>before</em> the bar fills: only an all-washed
     * batch needs the full timer, because Perfect is the one grade gated on it.
     *
     * <p>Fill level would be useless here: the output slot holds one item whether the batch is a
     * rushed Rough or a finished Perfect, so a stock container comparator reads the same either way
     * and cannot tell a player anything they want to know. Reporting state instead of fullness is
     * well-trodden vanilla ground — cauldrons report water level, composters their fill stage, cake
     * its bites, beehives their honey, respawn anchors their charge.
     *
     * <p>What it buys: a lamp or note block that fires the moment a batch is worth collecting, or
     * redstone that gates something else on it. (Extraction itself needs no redstone — the spout
     * already waits, see {@link #pushOutput}.)
     *
     * <p>The climb scales against {@link #fullTime()}, not the default: scaling it against the
     * hardcoded 18000 while the screen scales its bar against the configured simmer would leave a
     * tuned server's redstone and its GUI disagreeing about the same batch — which is precisely the
     * "one definition of done across the spout, the comparator and the hopper" this method exists
     * to uphold.
     */
    public int getComparatorOutput() {
        if (progress <= 0) {
            return 0;
        }
        if (isAtBestQuality()) {
            return 15;
        }
        return 1 + Math.min(13, progress * 13 / fullTime());
    }

    // ----- inventory -----

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    /**
     * Only the two hemp slots take anything, and both take either type. There are two of them so a
     * batch can <em>mix</em> washed and unwashed — which is the only way to reach
     * {@link Quality#CLEAN} — not so each type has a dedicated home. The output is take-only, and
     * the two retired slots take nothing ever again.
     */
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot >= FIRST_HEMP_SLOT && slot < FIRST_HEMP_SLOT + HEMP_SLOT_COUNT && isHemp(this.world, stack);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return switch (side) {
            case DOWN -> new int[]{OUTPUT_SLOT};
            default -> new int[]{FIRST_HEMP_SLOT, FIRST_HEMP_SLOT + 1};
        };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        // Automation only ever gets a batch at its best grade. Without this a hopper would snatch
        // the Rough preview the instant it appeared, making an automated Infuser strictly worse
        // than a hand-tended one — the opposite of what automation should buy you.
        // Kept in step with the spout deliberately: two definitions of "done" would be a trap.
        // (Unreachable by a hopper — see pushOutput for why none can sit under this block — but a
        // pipe reaches it through the Transfer API.)
        return slot == OUTPUT_SLOT && isAtBestQuality();
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

    // ReadView/WriteView since 1.21.9; the keys are unchanged, so older worlds still load.
    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, inventory);
        view.putInt("Progress", progress);
        view.putBoolean("HaveMilk", haveMilk);
        view.putInt("BatchUnwashed", batchUnwashed);
        view.putInt("BatchWashed", batchWashed);
        view.putBoolean("PreviewShown", previewShown);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        inventory.clear();
        Inventories.readData(view, inventory);
        progress = view.getInt("Progress", 0);
        haveMilk = view.getBoolean("HaveMilk", false);
        batchUnwashed = view.getInt("BatchUnwashed", 0);
        batchWashed = view.getInt("BatchWashed", 0);
        // Absent in worlds written before this field existed, and false is what those meant.
        previewShown = view.getBoolean("PreviewShown", false);
    }
}
