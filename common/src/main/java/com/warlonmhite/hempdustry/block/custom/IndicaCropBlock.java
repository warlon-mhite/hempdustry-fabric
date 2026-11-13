package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.advancement.HarvestHempCriterion;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * A two-tall crop. Redesigned around the vanilla
 * double-block pattern: a {@link #HALF} property (LOWER/UPPER) plus the standard
 * {@code age 0..7}. The canonical age lives on the LOWER half; the UPPER half
 * mirrors it. The plant is planted as a single LOWER stalk and grows into its
 * two-tall form once the lower half reaches {@link #DOUBLE_BLOCK_AGE}.
 *
 * <h2>A plant that hangs</h2>
 *
 * A subclass whose {@link #hangs} is {@code true} grows the same plant upside down, from a ceiling,
 * the way weeping vines do. Everything above then holds with the halves swapped: the <b>root</b> is
 * the half on the block the plant grows from — the LOWER of a standing plant, the UPPER of a hanging
 * one — and the root carries the age, the trim flags and the light record. The root has to be the
 * anchored half, never the tip: with the tip canonical, losing the tip would leave an anchored stump
 * with no age at all. Every reference below to "the lower half" means the root.
 */
public class IndicaCropBlock extends CropBlock implements Mildew.Host, EntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    /** Highest age value; a lower half at this age is mature. Matches {@code BlockStateProperties.AGE_7}. */
    public static final int MAX_AGE = 7;

    /** The lower half sprouts an upper half once it reaches this age (and there is room above). */
    public static final int DOUBLE_BLOCK_AGE = 4;

    /** One in this many bonemeal applications does nothing (but is still consumed). */
    public static final int BONEMEAL_FAILURE_CHANCE = 3;

    private static final VoxelShape[] LOWER_SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    private static final VoxelShape UPPER_SHAPE =
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    /** {@link #LOWER_SHAPE_BY_AGE} turned upside down, for a root hanging from the block above. */
    private static final VoxelShape[] HANGING_ROOT_SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 10.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 6.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 3.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 1.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    private static final VoxelShape HANGING_TIP_SHAPE =
            Block.box(0.0D, 4.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public IndicaCropBlock(Properties settings) {
        super(settings);
        // Defoliation.unworked is load-bearing: a BooleanProperty left out of the default state
        // resolves to *true*, which would plant every seed pre-trimmed. See its javadoc.
        this.registerDefaultState(Defoliation.unworked(this.stateDefinition.any()
                .setValue(this.getAgeProperty(), 0)
                .setValue(HALF, this.rootHalf())));
    }

    /**
     * Whether this plant hangs from a ceiling and grows downward instead of standing on a floor. Read
     * in the constructor, so an override must return a constant.
     */
    protected boolean hangs() {
        return false;
    }

    /** The half that carries the plant: on the block it grows from, and canonical. */
    private DoubleBlockHalf rootHalf() {
        return this.hangs() ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;
    }

    /** The half the root grows into, once the plant is tall enough for one. */
    private DoubleBlockHalf tipHalf() {
        return this.hangs() ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER;
    }

    /** The way the plant grows from its root: up from a floor, or down from a ceiling. */
    Direction growth() {
        return this.hangs() ? Direction.DOWN : Direction.UP;
    }

    /** Whether this state is the plant's root, where the age and the trim flags live. */
    boolean isRoot(BlockState state) {
        return state.getValue(HALF) == this.rootHalf();
    }

    /** The block a root at {@code rootPos} grows from: its soil, or its ceiling. */
    private BlockPos anchorOf(BlockPos rootPos) {
        return rootPos.relative(this.growth().getOpposite());
    }

    /** The root of the plant that {@code state} at {@code pos} belongs to, if it is a whole plant. */
    private BlockPos rootOf(BlockPos pos, BlockState state) {
        return this.isRoot(state) ? pos : pos.relative(this.growth().getOpposite());
    }

    private BlockState stateFor(int age, DoubleBlockHalf half) {
        return this.defaultBlockState().setValue(this.getAgeProperty(), age).setValue(HALF, half);
    }

    /**
     * A half above the LOWER, carrying a copy of the plant's records — its light and its mildew —
     * from {@code lower}. The records are canonical on the LOWER, but whatever draws a block reads
     * only that block — a colour provider, and a blockstate model can do nothing else — so without
     * the copy a stressed plant would be drawn stressed at the foot and healthy at the top.
     */
    private BlockState stateFor(int age, DoubleBlockHalf half, BlockState lower) {
        return this.stateFor(age, half).setValue(GrowLight.PROPERTY, lower.getValue(GrowLight.PROPERTY))
                .setValue(Mildew.PROPERTY, lower.getValue(Mildew.PROPERTY));
    }

    /** Farmland, or one of the mod's beds — a Grow Pot or a Hydro Tray. */
    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return super.mayPlaceOn(floor, world, pos) || floor.getBlock() instanceof GrowMedium;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.INDICA_SEEDS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (!this.isRoot(state)) {
            return this.hangs() ? HANGING_TIP_SHAPE : UPPER_SHAPE;
        }
        VoxelShape[] byAge = this.hangs() ? HANGING_ROOT_SHAPE_BY_AGE : LOWER_SHAPE_BY_AGE;
        return byAge[Math.min(this.getAge(state), byAge.length - 1)];
    }

    // ----- support / placement -----
    // The tip only survives right beside a root of this block old enough to have grown one, on the side
    // away from the anchor. The root uses the standard crop rule (enough light + the right ground), which
    // also prevents seeds from being placed onto an existing plant. VegetationBlock's updateShape
    // (inherited) breaks either half as soon as canSurvive fails, so removing the root cascades to the
    // tip -- and removing the anchor cascades to both, the root first. The age matters when another mod
    // harvests and replants the root inside a block update (Just Dire Things' goo soil): the update that
    // breaks the old tip is queued until the outer one ends, and by then a seedling stands under it. Only
    // its age tells that this tip is not its own.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        if (!this.isRoot(state)) {
            BlockState root = world.getBlockState(this.rootOf(pos, state));
            return root.is(this) && this.isRoot(root) && this.getAge(root) >= DOUBLE_BLOCK_AGE;
        }
        if (!this.needsLight() || this.hangs()) {
            // CropBlock's rule is light 8 and the soil below; a plant that needs no light keeps the
            // ground, and a hanging one reads it above instead.
            BlockPos anchor = this.anchorOf(pos);
            return (!this.needsLight() || CropBlock.hasSufficientLight(world, pos))
                    && this.mayPlaceOn(world.getBlockState(anchor), world, anchor);
        }
        return super.canSurvive(state, world, pos);
    }

    /**
     * A root old enough to carry its tip goes when the tip is taken by anything but the plant's own rules
     * ({@link TallHemp#lostItsTop}): a farmer that reaps the top, a harvester that sweeps at the top's height,
     * a piston, water. The game breaks it with its drops, so the plant's harvest falls once, as when a player
     * breaks either half; and a tip written back beside the root afterwards (Create's harvester replants what
     * it cut where it can survive) finds nothing to hold on to.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        BlockState shaped = super.updateShape(state, world, ticks, pos, direction, neighborPos, neighbor, random);
        if (direction == this.growth() && shaped == state
                && TallHemp.lostItsTop(this, world, pos, this.isRoot(state) && this.getAge(state) >= DOUBLE_BLOCK_AGE)) {
            return Blocks.AIR.defaultBlockState();
        }
        return shaped;
    }

    /**
     * An explosion that reaches the tip takes the root with it, as that explosion: the harvest goes through the
     * explosion's own drops, with its decay, rather than whole through the root's shape update.
     */
    @Override
    protected void onExplosionHit(BlockState state, ServerLevel world, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> drops) {
        if (!this.isRoot(state) && this.canSurvive(state, world, pos)) {
            BlockPos rootPos = this.rootOf(pos, state);
            world.getBlockState(rootPos).onExplosionHit(world, rootPos, explosion, drops);
        }
        super.onExplosionHit(state, world, pos, explosion, drops);
    }

    /** Whether the plant needs light to survive and grow. Warped Kush, like nether wart, does not. */
    protected boolean needsLight() {
        return true;
    }

    /** The divisor in the growth roll; vanilla wheat's 25 here. */
    public float growthResistance(LevelReader world, BlockPos pos) {
        return 25.0F;
    }

    /**
     * The moisture the growth roll reads when the plant stands in the open ground rather than in a
     * bed of ours: vanilla's farmland count. <b>{@code 0} means "does not grow this tick"</b> — the
     * roll is skipped rather than divided by zero. Beldía overrides this, since it grows on sand.
     */
    public float fieldMoisture(LevelReader world, BlockPos pos) {
        return Services.PLATFORM.cropGrowthSpeed(defaultBlockState(), world, pos);
    }

    /** Whether each growth step writes the light record. Warped Kush keeps none. */
    protected boolean recordsLight() {
        return true;
    }

    /**
     * Whether a light at night stresses the plant: the light leak ({@link PlantStress#touchyStep}), every
     * plant of the strain alike. White Widow only, the one strain whose most likely light step is very weak.
     */
    protected boolean leaksAtNight() {
        return false;
    }

    // ----- growth -----
    // Only the root random-ticks; it drives the whole plant, and bone meal on either half feeds it.
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        // Deliberately not gated on `age < maxAge`: the tick is also what repairs a plant whose
        // upper half is missing or stale (see randomTick), which can happen at max age too. Vanilla
        // crops do stop here, so this is a cost vanilla does not pay — which is why randomTick
        // checks isShapeSettled before writing anything, keeping a settled plant to one block read.
        return this.isRoot(state);
    }

    /**
     * Either half answers for the plant, from its root, as vanilla's pitcher crop does: other mods' harvesters
     * read "refuses bone meal" as "ripe" on any block they reach (a farm controller cutting at the height of
     * the tops), so a tip that refused it was cut off a growing plant.
     *
     * <p>Bonemeal is refused — and so not consumed — once the plant has grown as far as its headroom
     * allows. Without the {@link #maxAgeFor} half of this test, bonemeal would happily push a plant
     * that is boxed in all the way to maturity as a lone stalk: {@code growCrops} writes the
     * lower half's age whether or not the upper half can be placed, and unlike the random tick it
     * has no light check to incidentally stop it.
     */
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, BonemealSource source) {
        BlockPos rootPos = TallHemp.rootOf(this, world, pos, state);
        return rootPos != null && this.getAge(world.getBlockState(rootPos)) < this.maxAgeFor(world, rootPos);
    }

    /**
     * The highest age this plant may grow to at {@code pos} given what is above it (below it, for a
     * hanging plant). A plant needs that block free from {@link #DOUBLE_BLOCK_AGE} onwards, so one
     * that is boxed in stops one stage short of that and waits. Air only: a hanging plant over lava
     * or water waits too, rather than dangling its tip into it.
     *
     * <p>Holding the <em>age</em> back is what keeps the plant honest, rather than letting the age
     * run ahead and reconciling the shape afterwards: age is what the loot table and the models
     * read, so an age-7 lone stalk would look and harvest like a finished plant that had never
     * needed the second block at all.
     */
    private int maxAgeFor(LevelReader world, BlockPos pos) {
        return canOccupy(world, pos.relative(this.growth())) ? this.getMaxAge() : DOUBLE_BLOCK_AGE - 1;
    }

    /** Whether the plant may grow into {@code pos} — empty space, or a half it already owns. */
    private boolean canOccupy(LevelReader world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || (state.is(this) && !this.isRoot(state));
    }

    // No getAge override: the tip reports its own age, which setAge keeps level with the root's.
    // Harvesters in other mods (Industrial Foregoing's, Create's) take any CropBlock whose isMaxAge
    // holds, so a tip that claimed to be ripe was cut off a growing plant. Bees, which would otherwise
    // rebuild a tip as a root, are kept to the root by BeeGrowCropsGoalMixin instead.

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!this.isRoot(state)) {
            return;
        }
        int age = this.getAge(state);
        int grown = age;
        if (age < this.getMaxAge() && (!this.needsLight() || world.getRawBrightness(pos, 0) >= 9)) {
            // The ground answers for itself when it is a bed of ours (a pot or a tray): its own
            // moisture, and no crowding penalty, because the plant has soil or solution to itself.
            BlockState floor = world.getBlockState(this.anchorOf(pos));
            float moisture;
            float bed = 1.0F;
            if (floor.getBlock() instanceof GrowMedium medium) {
                moisture = medium.moisture(floor);
                bed = medium.speed(floor);
            } else {
                moisture = this.fieldMoisture(world, pos);
            }
            boolean rolled = moisture > 0.0F && HempGrowth.rolls(random, this.growthResistance(world, pos), moisture,
                    bed * state.getValue(GrowLight.PROPERTY).speedAt(age));
            if (Services.PLATFORM.canCropGrow(world, pos, state, rolled)) {
                grown++;
            }
        }
        // Reconcile whether or not the plant just aged. setAge both syncs the upper half's age and
        // sprouts it when missing, so this repairs a plant that was boxed in earlier and now has
        // headroom, or one whose lower half a bee bumped with a raw setBlockAndUpdate that never went
        // through setAge.
        //
        // Guarded on there being something to do, though, because this block random-ticks for ever
        // -- isRandomlyTicking is not gated on maturity -- and a finished field is the normal end state
        // of a hemp farm. Calling setAge unconditionally cost two Level#setBlock calls and a
        // maxAgeFor probe on every tick of every mature plant; the writes did nothing (an identical
        // state early-outs in LevelChunk#setBlockState) but each still paid a chunk lookup.
        // isShapeSettled answers the same question in one block read.
        if (grown != age || !this.isShapeSettled(world, pos, age, state)) {
            this.setAge(world, pos, world.getBlockState(pos), grown, StressCause.NONE, random);
        }
        // Told from the age written, not the roll: a plant held back by its headroom rolls as often as
        // any other while writeAge keeps it where it is, and a listener paid per growth (Ars Nouveau's
        // Agronomic Sourcelink) would be paid for a roofed seedling for ever. The root is read back
        // because a harvesting soil may take the plant inside the write; a new seedling there differs
        // from the old age too, which is the truth: it grew, and was taken.
        BlockState after = world.getBlockState(pos);
        if (after.is(this) && this.isRoot(after) && this.getAge(after) != age) {
            Services.PLATFORM.cropGrew(world, pos, state);
        }
        Mildew.randomTick(world, pos, random);
    }

    /**
     * Whether the plant at {@code pos} already carries the upper half its age calls for, so a
     * reconciling {@link #setAge} at the same age would write nothing above the lower half.
     *
     * <p>Deliberately built from {@link #stateFor} and the same {@code upperPresent}/{@code isAir}
     * test {@link #setAge} branches on, rather than restating the rule: this has to mean <em>exactly
     * </em> "setAge would be a no-op", and two separately-worded versions of that would drift.
     * (The lower half needs no check — setAge writes it back through
     * {@link Defoliation#carryOver}, which at an unchanged age reproduces the state that is already
     * there.) {@code lower} is the lower half, whose records the upper half copies.
     */
    private boolean isShapeSettled(LevelReader world, BlockPos pos, int age, BlockState lower) {
        BlockState above = world.getBlockState(pos.relative(this.growth()));
        if (above == this.stateFor(age, this.tipHalf(), lower)) {
            return true;
        }
        // An upper half at a stale age, or with a stale copy of the record, needs syncing; a gap
        // needs sprouting once the plant is tall enough for one. Anything else — a boxed-in plant,
        // or one too young — is already settled.
        boolean upperPresent = above.is(this) && above.getValue(HALF) == this.tipHalf();
        return !upperPresent && !(age >= DOUBLE_BLOCK_AGE && above.isAir());
    }

    // Bonemeal path: CropBlock.performBonemeal() calls growCrops() on the targeted block, which may be either
    // half; both grow the plant from its root. performBonemeal is overridden as well so the step uses the
    // random it is handed, which is also what lets a test decide the roll.
    @Override
    public void growCrops(Level world, BlockPos pos, BlockState state) {
        this.boneMeal(world, pos, state, world.getRandom());
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        this.boneMeal(world, pos, state, random);
    }

    /** One bone meal step, from the root — and, on a flowering plant in a fed bed, a chance of overfeeding it. */
    private void boneMeal(Level world, BlockPos pos, BlockState state, RandomSource random) {
        BlockPos rootPos = TallHemp.rootOf(this, world, pos, state);
        if (rootPos == null) {
            return;
        }
        BlockState root = world.getBlockState(rootPos);
        this.setAge(world, rootPos, root, this.getAge(root) + this.getBonemealAgeIncrease(world),
                PlantStress.ofBoneMeal(world, rootPos, root), random);
    }

    /**
     * How many stages a single successful bonemeal advances the plant. Vanilla crops
     * use 2-5 (maturing in ~2 applications); indica advances a flat 1 stage. Keeping
     * it at exactly 1 (never 2+) sets a hard floor of {@code MAX_AGE} lucky bonemeals
     * to mature, and combined with the {@link #BONEMEAL_FAILURE_CHANCE} 1-in-3 wasted
     * applications this works out to roughly 7-14 bonemeal from seed to harvest
     * (mean ~10.5) for the current 8-stage cycle. Revisit alongside the natural
     * growth cycle if MAX_AGE is reworked.
     */
    @Override
    protected int getBonemealAgeIncrease(Level world) {
        return 1;
    }

    /**
     * Whether a bonemeal application actually grows the plant. Bonemeal is consumed
     * regardless of this result (see {@code BoneMealItem.useOnFertilizable}), so
     * returning false here means the application is wasted. Indica has a
     * {@code 1 / BONEMEAL_FAILURE_CHANCE} chance of that happening, making bonemeal
     * an unreliable nudge rather than a fast path to harvest for this slow plant.
     */
    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return random.nextInt(BONEMEAL_FAILURE_CHANCE) != 0;
    }

    /**
     * Sets the lower half to {@code newAge} and keeps the upper half in sync: it mirrors the age
     * when present, and is sprouted once the plant reaches {@link #DOUBLE_BLOCK_AGE} and there is
     * room above.
     *
     * <p>{@code newAge} is clamped to {@link #maxAgeFor}, so a plant with something sitting on its
     * head stalls one stage short of needing that block rather than ageing on as a lone stalk. It
     * resumes by itself once the space is cleared, because randomTick calls this on every tick.
     *
     * <p>The clamp deliberately never lowers an age the plant already has: a finished plant that a
     * player later builds over keeps its age and its harvest instead of silently reverting.
     *
     * <p>{@code current} is the plant the step starts from: the block at {@code pos}, except when
     * {@link #onPlace} replays a raw write, where it is the state that write replaced.
     */
    private void setAge(Level world, BlockPos pos, BlockState current, int newAge, StressCause extraStress,
                        RandomSource random) {
        TallHemp.ownWrite(() -> {
            this.writeAge(world, pos, current, newAge, extraStress, random);
            return true;
        });
    }

    private void writeAge(Level world, BlockPos pos, BlockState current, int newAge, StressCause extraStress,
                          RandomSource random) {
        int currentAge = current.is(this) && this.isRoot(current) ? this.getAge(current) : 0;
        newAge = Math.min(newAge, this.getMaxAge());
        newAge = Math.min(newAge, Math.max(currentAge, this.maxAgeFor(world, pos)));

        // stateFor rebuilds from defaultBlockState(), so the lower half's trim flags have to be
        // carried across explicitly or every growth step would quietly wipe them — and randomTick
        // calls this on every tick, not only when the plant actually ages.
        BlockState lower = Defoliation.carryOver(current, this.stateFor(newAge, this.rootHalf()));
        if (newAge > currentAge && current.is(this)) {
            if (this.recordsLight()) {
                lower = lower.setValue(GrowLight.PROPERTY, PlantStress.afterStep(world, pos,
                        current.getValue(GrowLight.PROPERTY), currentAge, newAge, 2, extraStress, random, true,
                        this.leaksAtNight()));
            }
            lower = lower.setValue(Mildew.PROPERTY, Mildew.afterStep(world, pos, current,
                    current.getValue(Mildew.PROPERTY), random));
            if (newAge == this.getMaxAge()) {
                BlockPos floorPos = this.anchorOf(pos);
                BlockState floor = world.getBlockState(floorPos);
                if (floor.getBlock() instanceof GrowMedium medium) {
                    medium.spend(world, floorPos, floor);
                }
            }
        }
        world.setBlock(pos, lower, Block.UPDATE_CLIENTS);
        if (world.getBlockState(pos) != lower) {
            // Something answered that write by taking the plant: a soil that harvests what ripens on it
            // (Just Dire Things' goo soil) hears the shape update and replants before this returns. The
            // tip is no longer this plant's to grow; written now, it would stand ripe on a seedling.
            // Nor will the old one fall by itself: the update that would break it was raised inside this
            // write's own, so the game queues it, and by the time it runs a new root stands beside it again.
            this.removeTip(world, pos);
            return;
        }

        BlockPos upPos = pos.relative(this.growth());
        BlockState above = world.getBlockState(upPos);
        boolean upperPresent = above.is(this) && above.getValue(HALF) == this.tipHalf();
        if (upperPresent) {
            world.setBlock(upPos, this.stateFor(newAge, this.tipHalf(), lower), Block.UPDATE_CLIENTS);
        } else if (newAge >= DOUBLE_BLOCK_AGE && above.isAir()) {
            world.setBlock(upPos, this.stateFor(newAge, this.tipHalf(), lower), Block.UPDATE_CLIENTS);
        }
    }

    // ----- defoliation -----

    /**
     * Shearing a growing plant takes a fan leaf and shifts its eventual harvest towards buds. The
     * work is shared with the sativa crop in {@link Defoliation}; all this override does is resolve
     * whichever half the player clicked down to the lower one, where the age and the trim flags
     * live.
     *
     * <p>This is {@code useItemOn} rather than {@code useWithoutItem} because the player is holding an
     * item: 1.21 offers the held stack and hand directly here, which is also what makes shearing
     * work from the off-hand.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos lowerPos = this.rootOf(pos, state);
        BlockState lower = world.getBlockState(lowerPos);
        if (lower.is(this) && this.isRoot(lower)) {
            // A bottle on the plant waters its pot: that is where a player aims.
            InteractionResult watered = GrowPotBlock.tryWater(stack, world, this.anchorOf(lowerPos), player, hand);
            if (watered != null) {
                return watered;
            }
            InteractionResult result = Defoliation.tryCut(world, lowerPos, lower, lower.getValue(AGE),
                    stack, player, hand);
            if (result != null) {
                return result;
            }
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    // ----- the smell of a ripe plant -----

    /**
     * A ripe plant lets off a wisp now and then from its top; see {@link RipeAroma}. Either half
     * can be sampled, and the helper keeps only the one with open air above it.
     *
     * <p>The tip's {@link #AGE} mirrors the root's, so either half answers truthfully here.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= this.getMaxAge()) {
            RipeAroma.randomDisplayTick(world, pos, random, this.growth());
        }
        Mildew.animateTick(state, world, pos, random);
    }

    // ----- harvesting -----
    // Breaking either half removes both and yields the plant exactly once: the loot
    // table only drops for the lower half, so we drop the lower's loot (when an upper
    // half is broken directly) and remove the partner without letting it drop. The
    // partner goes as the plant's own write, so that a ripe root losing its tip
    // here is not taken for a plant whose top something else took (updateShape).
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
            TallHemp.ownWrite(() -> {
                this.takePartner(world, pos, state, player);
                return true;
            });
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    private void takePartner(Level world, BlockPos pos, BlockState state, Player player) {
        // The trim flags and the canonical age both live on the root, so the harvest
        // criterion is fed from there whichever half the player actually broke.
        BlockPos canonicalPos = this.rootOf(pos, state);
        BlockState canonical = world.getBlockState(canonicalPos);
        if (canonical.is(this) && this.isRoot(canonical)
                && this.isMaxAge(canonical)) {
            HarvestHempCriterion.trigger(player, (ServerLevel) world, canonicalPos, canonical);
        }

        if (!this.isRoot(state)) {
            BlockPos lowerPos = canonicalPos;
            BlockState lower = canonical;
            if (lower.is(this) && this.isRoot(lower)) {
                if (!player.isCreative()) {
                    // With the root's record: the buds take the plant's band from it.
                    dropResources(lower, world, lowerPos, world.getBlockEntity(lowerPos), player,
                            player.getMainHandItem());
                }
                world.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        } else {
            BlockPos upperPos = pos.relative(this.growth());
            BlockState upper = world.getBlockState(upperPos);
            if (upper.is(this) && upper.getValue(HALF) == this.tipHalf()) {
                world.setBlock(upperPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }


    // ----- the plant's record -----

    /**
     * The plant's record ({@link PlantRecordBlockEntity}) sits on its root only: every other block of
     * the plant answers none.
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.keepsRecord() && this.isRoot(state) ? new PlantRecordBlockEntity(pos, state) : null;
    }

    /** Whether this crop keeps a record of its own rolls: every crop that grows buds. */
    protected boolean keepsRecord() {
        return true;
    }

    /**
     * A root set back to age 0 in place — by a harvester that replants, through vanilla's
     * {@code getStateForAge}, or a click harvest that sets the age alone — is a new plant, and its record
     * rolls again. Then the write is put right if it came from another mod ({@link TallHemp#isRawWrite}): a
     * harvester that replants, an instant fertiliser, a growth soil or a bee that writes
     * {@code getStateForAge}. Every such write lands here, on both loaders, whoever made it, so no mod
     * needs a fix of its own.
     *
     * <ul>
     *   <li><b>Onto the tip</b>, the harvester took a ripe top for a crop of its own. The tip goes back as
     *       it was: the plant's loot is on the root, and a root there would break off. So does a tip whose
     *       age alone another mod set back ({@link TallHemp#isAgeWrittenAboveTheRoot}), as a right-click
     *       harvest does to the block clicked: it paid nothing, and a seedling tip would sit in a ripe plant
     *       until the root next grew. A write there that <b>raises</b> the age
     *       ({@link TallHemp#isRaisedAboveTheRoot}), an instant fertiliser clicked on the tip, puts the tip
     *       back too, and then grows the plant from its root to that age, as below.</li>
     *   <li><b>A younger age on the root</b> is a replanting. The written state is already a new plant's;
     *       what is left of the old one beside it goes, its harvest having been taken from the root. A mod
     *       that sets the age alone back ({@link TallHemp#isSetBackByAge}), as a right-click harvest
     *       replants, keeps only the trims and the rub the new age could have earned: a seedling keeps
     *       none.</li>
     *   <li><b>An older age</b> is growth, taken as one step of the plant's own: the trims, the rub and the
     *       light record carry over, the light and the ground are read, a pot is eaten from on ripening, the
     *       plant stops short of a block in its way, and the tip grows at once.</li>
     *   <li><b>The same age</b> is left as written. No harvester, fertiliser or soil writes a plant's own age
     *       back onto it; {@code /setblock} does, and means a fresh plant by it.</li>
     * </ul>
     */
    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, world, pos, old, moved);
        if (world.isClientSide()) {
            return;
        }
        if (old.is(this) && this.isRoot(old) && this.isRoot(state)
                && old.getValue(AGE) > 0 && state.getValue(AGE) == 0) {
            PlantRecordBlockEntity.replantAt(world, pos);
        }
        boolean raw = TallHemp.isRawWrite(this, state, old);
        if (raw && !this.isRoot(old) || TallHemp.isAgeWrittenAboveTheRoot(this, state, old)) {
            boolean raised = TallHemp.isRaisedAboveTheRoot(this, state, old);
            TallHemp.ownWrite(() -> world.setBlock(pos, old, Block.UPDATE_CLIENTS));
            BlockPos rootPos = raised ? TallHemp.rootOf(this, world, pos, old) : null;
            if (rootPos != null) {
                BlockState root = world.getBlockState(rootPos);
                this.setAge(world, rootPos, root, this.getAge(state), StressCause.NONE, world.getRandom());
            }
            return;
        }
        if (!raw) {
            if (TallHemp.isSetBackByAge(this, state, old)) {
                BlockState earned = Defoliation.onlyEarned(state, this.getAge(state));
                if (earned != state) {
                    TallHemp.ownWrite(() -> world.setBlock(pos, earned, Block.UPDATE_CLIENTS));
                }
            }
            return;
        }
        int age = this.getAge(state);
        if (age == this.getAge(old)) {
            return; // a plant set afresh where it stood, as /setblock does: nothing grew, nothing to keep
        }
        boolean replanted = age < this.getAge(old);
        if (replanted) {
            this.removeTip(world, pos);
        }
        this.setAge(world, pos, replanted ? state : old, age, StressCause.NONE, world.getRandom());
    }

    /**
     * Takes away the tip growing from the root at {@code pos}, without drops: its plant's harvest is the
     * root's, and whoever replaced the root has had it.
     */
    private void removeTip(Level world, BlockPos pos) {
        BlockPos tipPos = pos.relative(this.growth());
        BlockState tip = world.getBlockState(tipPos);
        if (tip.is(this) && !this.isRoot(tip)) {
            TallHemp.ownWrite(() -> world.setBlock(tipPos, Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS));
        }
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF, Defoliation.TRIMMED_EARLY, Defoliation.TRIMMED_LATE, Defoliation.RUBBED,
                GrowLight.PROPERTY, Mildew.PROPERTY);
    }
}
