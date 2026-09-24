package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.advancement.HarvestHempCriterion;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A two-tall crop. Redesigned (approach B, see CLAUDE.md §8) around the vanilla
 * double-block pattern: a {@link #HALF} property (LOWER/UPPER) plus the standard
 * {@code age 0..7}. The canonical age lives on the LOWER half; the UPPER half
 * mirrors it. The plant is planted as a single LOWER stalk and grows into its
 * two-tall form once the lower half reaches {@link #DOUBLE_BLOCK_AGE}.
 */
public class IndicaCropBlock extends CropBlock {
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

    public IndicaCropBlock(Properties settings) {
        super(settings);
        // Defoliation.unworked is load-bearing: a BooleanProperty left out of the default state
        // resolves to *true*, which would plant every seed pre-trimmed. See its javadoc.
        this.registerDefaultState(Defoliation.unworked(this.stateDefinition.any()
                .setValue(this.getAgeProperty(), 0)
                .setValue(HALF, DoubleBlockHalf.LOWER)));
    }

    private static boolean isLower(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    private BlockState stateFor(int age, DoubleBlockHalf half) {
        return this.defaultBlockState().setValue(this.getAgeProperty(), age).setValue(HALF, half);
    }

    /**
     * A half above the LOWER, carrying a copy of the plant's light record. The record is canonical
     * on the LOWER, but whatever draws a block reads only that block — a colour provider, and a
     * blockstate model can do nothing else — so without the copy a stressed plant would be drawn
     * stressed at the foot and healthy at the top.
     */
    private BlockState stateFor(int age, DoubleBlockHalf half, GrowLight light) {
        return this.stateFor(age, half).setValue(GrowLight.PROPERTY, light);
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
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return UPPER_SHAPE;
        }
        return LOWER_SHAPE_BY_AGE[Math.min(this.getAge(state), LOWER_SHAPE_BY_AGE.length - 1)];
    }

    // ----- support / placement -----
    // The UPPER half only survives directly above a LOWER half of this block. The
    // LOWER half uses the standard crop rule (enough light + farmland below), which
    // also prevents seeds from being placed onto an existing plant. VegetationBlock's
    // updateShape (inherited) breaks either half as soon as canSurvive
    // fails, so removing the lower half cascades to the upper.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = world.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        if (!this.needsLight()) {
            // CropBlock's rule is light 8 and the soil; a plant that needs no light keeps the soil.
            BlockPos floor = pos.below();
            return this.mayPlaceOn(world.getBlockState(floor), world, floor);
        }
        return super.canSurvive(state, world, pos);
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
        return getGrowthSpeed(this, world, pos);
    }

    /** Whether each growth step writes the light record. Warped Kush keeps none. */
    protected boolean recordsLight() {
        return true;
    }

    // ----- growth -----
    // Only the lower half is fertile and random-ticks; it drives the whole plant.
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        // Deliberately not gated on `age < maxAge`: the tick is also what repairs a plant whose
        // upper half is missing or stale (see randomTick), which can happen at max age too. Vanilla
        // crops do stop here, so this is a cost vanilla does not pay — which is why randomTick
        // checks isShapeSettled before writing anything, keeping a settled plant to one block read.
        return isLower(state);
    }

    /**
     * Bonemeal is refused — and so not consumed — once the plant has grown as far as its headroom
     * allows. Without the {@link #maxAgeFor} half of this test, bonemeal would happily push a plant
     * that is boxed in all the way to maturity as a lone stalk: {@code growCrops} writes the
     * lower half's age whether or not the upper half can be placed, and unlike the random tick it
     * has no light check to incidentally stop it.
     */
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return isLower(state) && this.getAge(state) < this.maxAgeFor(world, pos);
    }

    /**
     * The highest age this plant may grow to at {@code pos} given what is above it. A plant needs
     * the block above free from {@link #DOUBLE_BLOCK_AGE} onwards, so one that is boxed in stops
     * one stage short of that and waits.
     *
     * <p>Holding the <em>age</em> back is what keeps the plant honest, rather than letting the age
     * run ahead and reconciling the shape afterwards: age is what the loot table and the models
     * read, so an age-7 lone stalk would look and harvest like a finished plant that had never
     * needed the second block at all.
     */
    private int maxAgeFor(LevelReader world, BlockPos pos) {
        return canOccupy(world, pos.above()) ? this.getMaxAge() : DOUBLE_BLOCK_AGE - 1;
    }

    /** Whether the plant may grow into {@code pos} — empty space, or a half it already owns. */
    private boolean canOccupy(LevelReader world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || (state.is(this) && !isLower(state));
    }

    /**
     * The plant's age as seen from this block. The canonical age lives on the lower half; the
     * upper half reports {@link #getMaxAge()} — "nothing left to grow here", which is true, since
     * the lower half drives the plant.
     *
     * <p>This is what makes the plant safe around <b>bees</b>. {@code Bee.BeeGrowCropGoal}
     * does {@code if (!isMaxAge(state)) setBlockAndUpdate(pos, getStateForAge(getAge(state) + 1))} directly on
     * any {@code CropBlock} in {@code #minecraft:bee_growables}, and {@link CropBlock#getStateForAge}
     * rebuilds the state from {@code defaultBlockState()} — resetting {@link #HALF} to LOWER. A bee
     * over a two-tall plant would turn its upper half into a lower half in mid-air, fail
     * {@code canSurvive} and behead the plant. {@code CropBlock#isMaxAge} is {@code final}, but it
     * is defined as {@code getAge(state) >= getMaxAge()}, so overriding {@code getAge} is how the
     * upper half is made to report itself mature and get skipped.
     *
     * <p>Safe because nothing reads the upper half's age: the outline shape, growth, fertilization
     * and harvesting all run on the lower half or go through {@link #HALF}, and the loot table and
     * blockstate read the {@link #AGE} property directly rather than this.
     */
    @Override
    public int getAge(BlockState state) {
        return isLower(state) ? super.getAge(state) : this.getMaxAge();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!isLower(state)) {
            return;
        }
        int age = this.getAge(state);
        int grown = age;
        if (age < this.getMaxAge() && (!this.needsLight() || world.getRawBrightness(pos, 0) >= 9)) {
            // The ground answers for itself when it is a bed of ours (a pot or a tray): its own
            // moisture, and no crowding penalty, because the plant has soil or solution to itself.
            BlockState floor = world.getBlockState(pos.below());
            float moisture;
            float bed = 1.0F;
            if (floor.getBlock() instanceof GrowMedium medium) {
                moisture = medium.moisture(floor);
                bed = medium.speed(floor);
            } else {
                moisture = this.fieldMoisture(world, pos);
            }
            if (moisture > 0.0F && HempGrowth.rolls(random, this.growthResistance(world, pos), moisture,
                    bed * state.getValue(GrowLight.PROPERTY).speedAt(age))) {
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
        if (grown != age || !this.isShapeSettled(world, pos, age, state.getValue(GrowLight.PROPERTY))) {
            this.setAge(world, pos, grown, 0.0F, random);
        }
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
     * there.) {@code light} is the lower half's record, which the upper half copies.
     */
    private boolean isShapeSettled(LevelReader world, BlockPos pos, int age, GrowLight light) {
        BlockState above = world.getBlockState(pos.above());
        if (above == this.stateFor(age, DoubleBlockHalf.UPPER, light)) {
            return true;
        }
        // An upper half at a stale age, or with a stale copy of the record, needs syncing; a gap
        // needs sprouting once the plant is tall enough for one. Anything else — a boxed-in plant,
        // or one too young — is already settled.
        boolean upperPresent = above.is(this) && above.getValue(HALF) == DoubleBlockHalf.UPPER;
        return !upperPresent && !(age >= DOUBLE_BLOCK_AGE && above.isAir());
    }

    // Bonemeal path: CropBlock.performBonemeal() calls growCrops() on the targeted block. Only the LOWER is
    // fertilizable, so this only ever runs on the LOWER. grow() is overridden as well so the roll
    // uses the random it is handed, which is also what lets a test decide the roll.
    @Override
    public void growCrops(Level world, BlockPos pos, BlockState state) {
        this.boneMeal(world, pos, state, world.getRandom());
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        this.boneMeal(world, pos, state, random);
    }

    /** One bone meal step — and, on a flowering plant in a fed bed, a chance of overfeeding it. */
    private void boneMeal(Level world, BlockPos pos, BlockState state, RandomSource random) {
        if (!isLower(state)) {
            return;
        }
        this.setAge(world, pos, this.getAge(state) + this.getBonemealAgeIncrease(world),
                PlantStress.ofBoneMeal(world, pos, state), random);
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
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
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
     */
    private void setAge(Level world, BlockPos pos, int newAge, float extraStress, RandomSource random) {
        BlockState current = world.getBlockState(pos);
        int currentAge = current.is(this) && isLower(current) ? this.getAge(current) : 0;
        newAge = Math.min(newAge, this.getMaxAge());
        newAge = Math.min(newAge, Math.max(currentAge, this.maxAgeFor(world, pos)));

        // stateFor rebuilds from defaultBlockState(), so the lower half's trim flags have to be
        // carried across explicitly or every growth step would quietly wipe them — and randomTick
        // calls this on every tick, not only when the plant actually ages.
        BlockState lower = Defoliation.carryOver(current, this.stateFor(newAge, DoubleBlockHalf.LOWER));
        if (newAge > currentAge && current.is(this)) {
            if (this.recordsLight()) {
                lower = lower.setValue(GrowLight.PROPERTY, PlantStress.afterStep(world, pos,
                        current.getValue(GrowLight.PROPERTY), currentAge, newAge, 2, extraStress, random, true));
            }
            if (newAge == this.getMaxAge()) {
                BlockPos floorPos = pos.below();
                BlockState floor = world.getBlockState(floorPos);
                if (floor.getBlock() instanceof GrowMedium medium) {
                    medium.spend(world, floorPos, floor);
                }
            }
        }
        world.setBlock(pos, lower, Block.UPDATE_CLIENTS);

        BlockPos upPos = pos.above();
        BlockState above = world.getBlockState(upPos);
        boolean upperPresent = above.is(this) && above.getValue(HALF) == DoubleBlockHalf.UPPER;
        GrowLight light = lower.getValue(GrowLight.PROPERTY);
        if (upperPresent) {
            world.setBlock(upPos, this.stateFor(newAge, DoubleBlockHalf.UPPER, light), Block.UPDATE_CLIENTS);
        } else if (newAge >= DOUBLE_BLOCK_AGE && above.isAir()) {
            world.setBlock(upPos, this.stateFor(newAge, DoubleBlockHalf.UPPER, light), Block.UPDATE_CLIENTS);
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
        BlockPos lowerPos = isLower(state) ? pos : pos.below();
        BlockState lower = world.getBlockState(lowerPos);
        if (lower.is(this) && isLower(lower)) {
            // A bottle on the plant waters its pot: that is where a player aims.
            InteractionResult watered = GrowPotBlock.tryWater(stack, world, lowerPos.below(), player, hand);
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
     * <p>Reads {@link #AGE} directly, <b>not</b> {@link #getAge}: the upper half's {@code getAge}
     * always reports maturity (the bee guard above), so asking it would have every two-tall plant
     * smelling ripe from age 4. The upper half's {@code AGE} mirrors the lower's, so either half
     * answers truthfully here.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= this.getMaxAge()) {
            RipeAroma.randomDisplayTick(world, pos, random);
        }
    }

    // ----- harvesting -----
    // Breaking either half removes both and yields the plant exactly once: the loot
    // table only drops for the lower half, so we drop the lower's loot (when an upper
    // half is broken directly) and remove the partner without letting it drop.
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
            // The trim flags and the canonical age both live on the LOWER half, so the harvest
            // criterion is fed from there whichever half the player actually broke.
            BlockPos canonicalPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockState canonical = world.getBlockState(canonicalPos);
            if (canonical.is(this) && canonical.getValue(HALF) == DoubleBlockHalf.LOWER
                    && this.isMaxAge(canonical)) {
                HarvestHempCriterion.trigger(player, (ServerLevel) world, canonicalPos, canonical);
            }

            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                BlockPos lowerPos = pos.below();
                BlockState lower = world.getBlockState(lowerPos);
                if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                    if (!player.isCreative()) {
                        dropResources(lower, world, lowerPos, null, player, player.getMainHandItem());
                    }
                    world.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            } else {
                BlockPos upperPos = pos.above();
                BlockState upper = world.getBlockState(upperPos);
                if (upper.is(this) && upper.getValue(HALF) == DoubleBlockHalf.UPPER) {
                    world.setBlock(upperPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF, Defoliation.TRIMMED_EARLY, Defoliation.TRIMMED_LATE, Defoliation.RUBBED,
                GrowLight.PROPERTY);
    }
}
