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
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
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
import net.minecraft.util.math.MathHelper;
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
 * <p><b>Ingredients are consumed as they enter the batch, not when the result is collected.</b> The
 * batch — {@link #haveMilk}, {@link #batchUnwashed}, {@link #batchWashed} — is bookkeeping on this
 * block entity rather than items sitting in slots. Two different rhythms:
 * <ul>
 *   <li><b>Milk goes in on contact, and only ever one at a time.</b> A bucket put in the slot is
 *       emptied into the tub that tick and its empty returned to {@link #BUCKET_SLOT} — but only if
 *       the tub is empty. <b>One milk buys one cannabutter:</b> the tub stays full for the whole
 *       batch and takes the next bucket only once the result has been collected. A second bucket
 *       parked in the milk slot meanwhile just <em>waits</em>, visibly, and is drawn in on the tick
 *       after collection. That is what the bucket-return slot is really for — without it the empty
 *       would sit in the milk slot and there would be nowhere to park the next one.</li>
 *   <li><b>Hemp dissolves gradually</b>, one item per {@link #ABSORB_INTERVAL}, and only until
 *       {@link #MIN_TIME}. That is what locks a batch: past the loading window the absorber has had
 *       all its turns, so nothing more goes in however much room is left.</li>
 * </ul>
 *
 * <p>Draining the hemp by {@code MIN_TIME} rather than over the whole simmer is load-bearing, not a
 * detail. <b>Strength is the whole batch, so the whole batch has to be paid for by the time the
 * result can first be taken.</b> Spread it over {@link #FULL_TIME} instead and an early pull leaves
 * a third-consumed batch, which can only be resolved by refunding the rest (full Strength for a
 * third of the hemp — an exploit), destroying it (the slots visibly empty for nothing), or scaling
 * Strength down (which makes rushing strictly dominated, and deletes it as a real choice).
 *
 * <p>What survives intact is both of the original promises. <b>Topping up still works</b> — hemp
 * added during the window is absorbed on the next interval — though late hemp genuinely cannot catch
 * up, which is the point. And <b>rushing still works</b>, because what the timer gates is the grade,
 * not the ingredients: from {@link #MIN_TIME} the output slot previews what this batch would yield
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

    public static final int MILK_SLOT = 0;
    /** The two interchangeable hemp slots are contiguous from here. */
    public static final int FIRST_HEMP_SLOT = 1;
    public static final int HEMP_SLOT_COUNT = 2;
    public static final int OUTPUT_SLOT = 3;
    /** Where emptied buckets are returned. Take-only; nothing may be inserted here. */
    public static final int BUCKET_SLOT = 4;
    public static final int SLOT_COUNT = 5;

    /**
     * Earliest a batch can be taken at all, and the zero point of {@link Quality}'s time dial. Six
     * in-game hours — the mod reuses Minecraft's own hour (1000 ticks) rather than inventing a
     * ratio, so "cannabutter takes hours" translates literally.
     */
    public static final int MIN_TIME = 6000;
    /**
     * A finished simmer: eighteen in-game hours, the midpoint of the real 12–24 hour range. There is
     * no benefit to leaving it longer — it just waits, like a grown crop.
     */
    public static final int FULL_TIME = 18000;

    /**
     * Most hemp one batch can hold. <b>Derived, not chosen:</b> it is exactly how many
     * {@link #ABSORB_INTERVAL}s fit in {@link #MIN_TIME}, so the cap is a consequence of the
     * dissolve rate rather than an independent number that has to be justified on its own.
     */
    public static final int BATCH_CAP = 24;

    /**
     * Ticks between one hemp dissolving into the batch and the next — {@code MIN_TIME / BATCH_CAP},
     * about 12.5 real seconds. Absorption runs only while the batch is simmering and stops dead at
     * {@link #MIN_TIME}, which is what makes the batch lock itself: after that the absorber has had
     * all the turns it is going to get, so nothing more can go in whatever room is left.
     *
     * <p><b>Exactly one per interval, never catching up.</b> Backfilling from
     * {@code progress / ABSORB_INTERVAL} would let hemp dropped in at tick 5999 be absorbed 23 at a
     * time, which throws away the point: reaching a full-strength batch should require the hemp to
     * have been <em>present</em> for the whole loading window. Load halfway through and you can only
     * reach 12.
     */
    public static final int ABSORB_INTERVAL = MIN_TIME / BATCH_CAP;

    /** Ticks between attempts to pour a finished batch out of the spout. A hopper's own cadence. */
    public static final int PUSH_COOLDOWN = 8;

    public static final int PROPERTY_PROGRESS = 0;
    public static final int PROPERTY_HEATED = 1;
    /** Washed share of the batch, 0–100, or -1 when there is no batch to grade. */
    public static final int PROPERTY_WASHED_PERCENT = 2;
    public static final int PROPERTY_COUNT = 3;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);

    private int progress;
    /** Whether something hot is underneath. Reported to the GUI's flame; not a blockstate on its own. */
    private boolean heated;
    /**
     * Whether there is milk in the tub. <b>One milk, one cannabutter</b> — this is a flag rather
     * than a counter on purpose: the tub takes a bucket only when it is empty, and stays full until
     * the batch is collected. Set by {@link #intakeMilk}, cleared by {@link #onPreviewTaken}.
     */
    private boolean haveMilk;
    /** Hemp already drawn into the running batch, by type. Together capped at {@link #BATCH_CAP}. */
    private int batchUnwashed;
    private int batchWashed;
    /**
     * Last value a comparator would have read. Progress changes every tick but its 0–15 projection
     * only changes about fifteen times a batch, so comparators are only poked when the number they
     * would report actually moves — updating them every tick would be twenty needless neighbour
     * updates a second.
     */
    private int lastComparatorLevel;
    /** Throttles {@link #pushOutput}. Not persisted — an 8-tick timer is not worth a save field. */
    private int pushCooldown;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_PROGRESS -> progress;
                case PROPERTY_HEATED -> heated ? 1 : 0;
                // Synced so the screen can work out when this batch's grade will next improve,
                // which under the score-based grading depends on the ratio and not just the clock.
                case PROPERTY_WASHED_PERCENT -> washedPercent();
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

    /** Either kind of decarboxylated hemp — both hemp slots accept both. */
    public static boolean isHemp(ItemStack stack) {
        return isUnwashedHemp(stack) || isWashedHemp(stack);
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

        // Milk is emptied into the tub the moment it is put in, heat or no heat, and its bucket is
        // returned straight away — so the milk slot is free for the player to park the *next*
        // bucket while this batch runs.
        if (intakeMilk()) {
            dirty = true;
        }

        if (canSimmer()) {
            // Losing the heat leaves progress where it is, so a campfire going out is a pause and
            // not a disaster. A batch that has run out of hemp entirely pauses the same way, and
            // resumes if more arrives before MIN_TIME.
            progress++;
            // Exactly one hemp per interval, and only during the loading window. See ABSORB_INTERVAL.
            if (progress <= MIN_TIME && progress % ABSORB_INTERVAL == 0) {
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

    /**
     * Empties a milk bucket into the tub the instant it is put in the slot and returns the empty
     * bucket to {@link #BUCKET_SLOT}. Deliberately independent of heat and of hemp: a bucket in the
     * slot is milk in the tub, which is what makes {@link #isFilled()} honest.
     *
     * <p><b>Only ever one at a time.</b> {@link #haveMilk} gates this, so a tub that already has milk
     * ignores further buckets entirely — one milk buys one cannabutter, and the machine cannot
     * silently swallow a stack of them. A second bucket parked in the slot simply <em>waits</em>,
     * visibly, and is taken up on the tick after the batch is collected. That is a better queue than
     * an invisible counter: what is pending is a real item you can see and take back.
     *
     * <p>{@link #hasBucketRoom()} is a second guard, for the case where the return slot has filled
     * with 16 empties — without somewhere to put the bucket, the milk would be taken and the bucket
     * destroyed.
     */
    private boolean intakeMilk() {
        ItemStack milk = getStack(MILK_SLOT);
        if (haveMilk || !isMilk(milk) || !hasBucketRoom()) {
            return false;
        }
        milk.decrement(1);
        ItemStack buckets = getStack(BUCKET_SLOT);
        if (buckets.isEmpty()) {
            // Vanilla's recipe-remainder mechanism only fires for real crafting recipes, never for a
            // block entity, so the bucket has to be handed back by hand.
            setStack(BUCKET_SLOT, new ItemStack(Items.BUCKET));
        } else {
            buckets.increment(1);
        }
        haveMilk = true;
        return true;
    }

    private boolean hasBucketRoom() {
        ItemStack buckets = getStack(BUCKET_SLOT);
        return buckets.isEmpty() || (buckets.isOf(Items.BUCKET) && buckets.getCount() < buckets.getMaxCount());
    }

    /**
     * Whether a batch can make progress this tick: hot, with milk in the tub, and with hemp to work
     * on — either already dissolved in or still in the slots.
     *
     * <p>The hemp clause is why a batch that has run out of hemp <em>pauses</em> rather than
     * finishing empty: without it a tub emptied at tick 100 would ride the clock to
     * {@link #MIN_TIME} with nothing in it, and the milk would be stranded on a batch that can never
     * produce a preview. Pausing also stops the absorption clock, so the window can't be run down
     * while there is nothing to put in it.
     *
     * <p><b>The {@link #isAtBestQuality} clause stops the clock the moment further simmering would
     * achieve nothing</b>, which keeps three things honest at once: the tub stops bubbling when the
     * batch is done rather than churning away at a finished result, the progress bar lands exactly on
     * full instead of overshooting a scale it has already left behind, and {@code progress} never
     * records time that meant anything. Safe from oscillating because the washed ratio is frozen
     * after {@link #MIN_TIME}, so once true this can never go back to false within a batch.
     */
    private boolean canSimmer() {
        return heated
                && haveMilk
                && (batchHemp() > 0 || availableHemp() > 0)
                && progress < FULL_TIME
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
                if (wantWashed ? isWashedHemp(stack) : isUnwashedHemp(stack)) {
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
            if (isHemp(stack)) {
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
     * Measured from {@link #MIN_TIME} rather than from zero, because nothing before that can be
     * graded at all — that is where the scale has to start for the score to mean anything.
     */
    public int timePercent() {
        int span = FULL_TIME - MIN_TIME;
        return MathHelper.clamp((progress - MIN_TIME) * 100 / span, 0, 100);
    }

    private boolean hasBatch() {
        return haveMilk && batchHemp() > 0;
    }

    /**
     * Whether the tub holds liquid. This drives {@link InfuserBlock#FILLED} and therefore the
     * block's texture, and is deliberately independent of heat: what makes the tub look full is milk
     * being in it, not something burning underneath. Since milk is emptied in on contact, it is true
     * from the tick the bucket lands in the slot and false again the moment the batch is collected —
     * so the texture is a direct readout of {@link #haveMilk} with nothing else mixed in.
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
        return progress >= MIN_TIME;
    }

    /**
     * The best grade this batch will ever reach — what it would earn at a full simmer.
     *
     * <p>This is knowable mid-simmer only because <b>the washed ratio is frozen after
     * {@link #MIN_TIME}</b>: absorption stops there, so nothing can change {@code washedPercent}
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

    /** The cannabutter this batch would yield right now, or empty if it isn't ready. */
    private ItemStack previewStack() {
        if (!isReady() || !hasBatch()) {
            return ItemStack.EMPTY;
        }
        ItemStack butter = new ItemStack(ModItems.CANNABUTTER);
        butter.set(ModComponents.STRENGTH, batchHemp());
        butter.set(ModComponents.QUALITY, Quality.of(timePercent(), washedPercent()));
        return butter;
    }

    /**
     * Pours a finished batch out of the spout into whatever inventory is against that face.
     * Returns whether anything moved.
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
     * <p>Only the cannabutter goes out here. Emptied buckets stay in their slot on purpose: at one
     * milk per batch the return slot holds sixteen batches' worth, which is hours of unattended
     * running, and splitting two item types across one spout would just hand the player a sorting
     * problem for no gain.
     *
     * <p>Pushing routes through {@link #removeStack}, so it closes the batch out exactly as a player
     * or a hopper taking it would — the one place that guarantee lives.
     */
    private boolean pushOutput(World world, BlockPos pos, BlockState state) {
        if (!isAtBestQuality() || getStack(OUTPUT_SLOT).isEmpty() || !state.contains(InfuserBlock.FACING)) {
            return false;
        }
        // A finished batch with nothing to pour into would otherwise re-scan every tick, and
        // getInventoryAt runs an entity query for inventory minecarts. Vanilla hoppers throttle the
        // same work to 8 ticks; so does this.
        if (pushCooldown > 0) {
            pushCooldown--;
            return false;
        }
        pushCooldown = PUSH_COOLDOWN;

        Direction facing = state.get(InfuserBlock.FACING);
        Inventory target = HopperBlockEntity.getInventoryAt(world, pos.offset(facing));
        if (target == null) {
            return false;
        }
        // transfer() only ever touches the destination and the stack handed to it — it never removes
        // from the source — so this passes a copy and does the removal itself.
        ItemStack pending = getStack(OUTPUT_SLOT).copy();
        int before = pending.getCount();
        // The receiving side is the face of the target that we are pouring into.
        ItemStack leftover = HopperBlockEntity.transfer(this, target, pending, facing.getOpposite());
        if (leftover.getCount() >= before) {
            return false;
        }
        // The preview is always a single item, so anything moving means all of it moved. The count
        // check above rather than isEmpty() keeps that from becoming a silent dupe if it ever isn't.
        removeStack(OUTPUT_SLOT);
        return true;
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
     * Closes the batch out: called when the player actually takes the preview from the output slot.
     * The ingredients were already spent as they were absorbed, so all this does is clear the batch
     * and start the timer over.
     *
     * <p>Clearing {@link #haveMilk} here is what makes the tub take milk again — the emptying of the
     * tub and the taking of the cannabutter are the same event, which is the whole "one milk, one
     * cannabutter" rule. If a bucket was parked in the milk slot it is drawn in on the very next
     * tick and the next batch begins by itself.
     */
    public void onPreviewTaken() {
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
     * the preview is not a real item yet: without this, breaking a tub at {@link #MIN_TIME} would
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
     * holds one at a time; a bucket parked in the milk slot is a normal item and spills normally.
     */
    public DefaultedList<ItemStack> getBatchItems() {
        DefaultedList<ItemStack> spill = DefaultedList.of();
        addBatchStacks(spill, ModItems.DECARBOXYLATED_HEMP, batchUnwashed);
        addBatchStacks(spill, ModItems.WASHED_DECARBOXYLATED_HEMP, batchWashed);
        return spill;
    }

    private static void addBatchStacks(DefaultedList<ItemStack> out, Item item, int count) {
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
     */
    public int getComparatorOutput() {
        if (progress <= 0) {
            return 0;
        }
        if (isAtBestQuality()) {
            return 15;
        }
        return 1 + Math.min(13, progress * 13 / FULL_TIME);
    }

    // ----- inventory -----

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    /**
     * Both hemp slots take either type. There are two of them so a batch can <em>mix</em> washed and
     * unwashed — which is the only way to reach {@link Quality#CLEAN} — not so each type has a
     * dedicated home.
     */
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == MILK_SLOT) {
            return isMilk(stack);
        }
        if (slot >= FIRST_HEMP_SLOT && slot < FIRST_HEMP_SLOT + HEMP_SLOT_COUNT) {
            return isHemp(stack);
        }
        // The output and the bucket return are both take-only: only the machine puts things there.
        return false;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return switch (side) {
            case DOWN -> new int[]{OUTPUT_SLOT, BUCKET_SLOT};
            default -> new int[]{MILK_SLOT, FIRST_HEMP_SLOT, FIRST_HEMP_SLOT + 1};
        };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        if (slot == OUTPUT_SLOT) {
            // Automation only ever gets a batch at its best grade. Without this a hopper would snatch
            // the Rough preview the instant it appeared, making an automated Infuser strictly worse
            // than a hand-tended one — the opposite of what automation should buy you.
            // Kept in step with the spout deliberately: two definitions of "done" would be a trap.
            // (Unreachable in practice — see pushOutput for why no hopper can sit under this block.)
            return isAtBestQuality();
        }
        // Emptied buckets are always drainable — and a hopper under the block is what keeps the
        // return slot from filling up and stalling the milk queue.
        return slot == BUCKET_SLOT;
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
        nbt.putBoolean("HaveMilk", haveMilk);
        nbt.putInt("BatchUnwashed", batchUnwashed);
        nbt.putInt("BatchWashed", batchWashed);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        inventory.clear();
        Inventories.readNbt(nbt, inventory, registryLookup);
        progress = nbt.getInt("Progress");
        haveMilk = nbt.getBoolean("HaveMilk");
        batchUnwashed = nbt.getInt("BatchUnwashed");
        batchWashed = nbt.getInt("BatchWashed");
    }
}
