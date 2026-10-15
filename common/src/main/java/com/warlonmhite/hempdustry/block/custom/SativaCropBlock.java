package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.advancement.HarvestHempCriterion;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Lemon Haze crop: a <em>three</em>-tall plant, built on the same pattern as the two-tall
 * {@link IndicaCropBlock} but with a {@link #SEGMENT} property (LOWER/MIDDLE/UPPER) in place of
 * vanilla's LOWER/UPPER {@code DoubleBlockHalf}, which can't describe three blocks.
 *
 * <p>As with indica the canonical {@code age 0..7} lives on the LOWER segment and the segments
 * above mirror it; only the LOWER random-ticks, is fertilizable, and drops loot. The plant gets
 * taller as it matures, which is the whole point of the strain — sativa needs headroom:
 *
 * <table>
 *   <tr><th>age</th><th>shape</th></tr>
 *   <tr><td>0–3</td><td>a single stalk (LOWER only)</td></tr>
 *   <tr><td>4–5</td><td>two tall (LOWER + UPPER)</td></tr>
 *   <tr><td>6–7</td><td>three tall (LOWER + MIDDLE + UPPER)</td></tr>
 * </table>
 *
 * <p>Reaching the next height needs the space free. If it isn't, the plant keeps ageing but stays
 * at the height it can fit and fills in later — the shape is reconciled against the age on every
 * growth step rather than being edge-triggered at one exact age, which is what made the pre-rewrite
 * indica crop unrecoverable (CLAUDE.md §8, finding #1).
 */
public class SativaCropBlock extends CropBlock {
    public static final EnumProperty<TriplePlantSegment> SEGMENT =
            EnumProperty.create("segment", TriplePlantSegment.class);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    /** Highest age value; a LOWER segment at this age is mature. Matches {@code BlockStateProperties.AGE_7}. */
    public static final int MAX_AGE = 7;

    /** From this age the plant wants a second block (LOWER + UPPER). */
    public static final int TWO_TALL_AGE = 4;
    /** From this age it wants a third (LOWER + MIDDLE + UPPER); the old UPPER becomes the MIDDLE. */
    public static final int THREE_TALL_AGE = 6;

    /** One in this many bonemeal applications does nothing (but is still consumed). */
    public static final int BONEMEAL_FAILURE_CHANCE = 3;

    /**
     * Divisor in the random-tick growth roll. Vanilla wheat — and indica — use 25.0F; the higher
     * value here makes each tick roughly 40% less likely to advance the plant, so Lemon Haze takes
     * noticeably longer to finish than Purple Kush. That mirrors the real thing (sativas flower
     * over a much longer season than indicas) and pays for the strain's better effects.
     */
    private static final float GROWTH_RESISTANCE = 35.0F;

    private static final VoxelShape[] LOWER_SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 5.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 11.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    private static final VoxelShape MIDDLE_SHAPE =
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape UPPER_SHAPE =
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D);

    public SativaCropBlock(Properties settings) {
        super(settings);
        // Defoliation.unworked is load-bearing: a BooleanProperty left out of the default state
        // resolves to *true*, which would plant every seed pre-trimmed. See its javadoc.
        this.registerDefaultState(Defoliation.unworked(this.stateDefinition.any()
                .setValue(this.getAgeProperty(), 0)
                .setValue(SEGMENT, TriplePlantSegment.LOWER)));
    }

    private static boolean isLower(BlockState state) {
        return state.getValue(SEGMENT) == TriplePlantSegment.LOWER;
    }

    private BlockState stateFor(int age, TriplePlantSegment segment) {
        return this.defaultBlockState().setValue(this.getAgeProperty(), age).setValue(SEGMENT, segment);
    }

    /**
     * A segment above the LOWER, carrying a copy of the plant's light record, for the same reason as
     * on {@link IndicaCropBlock}: whatever draws a block reads only that block.
     */
    private BlockState stateFor(int age, TriplePlantSegment segment, GrowLight light) {
        return this.stateFor(age, segment).setValue(GrowLight.PROPERTY, light);
    }

    /** Farmland, or one of the mod's beds — a Grow Pot or a Hydro Tray. */
    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return super.mayPlaceOn(floor, world, pos) || floor.getBlock() instanceof GrowMedium;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.SATIVA_SEEDS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(SEGMENT)) {
            case UPPER -> UPPER_SHAPE;
            case MIDDLE -> MIDDLE_SHAPE;
            case LOWER -> LOWER_SHAPE_BY_AGE[Math.min(this.getAge(state), LOWER_SHAPE_BY_AGE.length - 1)];
        };
    }

    // ----- support / placement -----
    // Every segment above the bottom only survives on top of the right partner, so removing any
    // one of them makes the ones above it unsupported. BushBlock's updateShape
    // (inherited) turns an unsupported segment into air, so the removal cascades upwards on its
    // own. The LOWER segment uses the standard crop rule (enough light + farmland below), which
    // is also what stops a seed being planted onto an existing plant.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        return switch (state.getValue(SEGMENT)) {
            // The UPPER sits on the LOWER while the plant is two tall, and on the MIDDLE once
            // it is three tall, so it accepts either.
            case UPPER -> below.is(this) && below.getValue(SEGMENT) != TriplePlantSegment.UPPER;
            case MIDDLE -> below.is(this) && below.getValue(SEGMENT) == TriplePlantSegment.LOWER;
            case LOWER -> super.canSurvive(state, world, pos);
        };
    }

    // ----- growth -----
    // Only the LOWER segment is fertile and random-ticks; it drives the whole plant.
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        // Deliberately *not* gated on `age < maxAge` the way vanilla's crops are. A plant that hit
        // age 7 while something was sitting two blocks above it is mature but a block short, and
        // this tick is what lets it finish once the space is cleared instead of staying stunted
        // forever. The extra ticks cost at most two block-state reads and no writes — see
        // isShapeSettled, which randomTick checks before touching anything.
        return isLower(state);
    }

    /**
     * Bonemeal is refused — and so not consumed — once the plant has grown as far as its headroom
     * allows. Without the {@link #maxAgeFor} half of this test, bonemeal would push a plant that is
     * boxed in all the way to maturity at whatever stunted height it happens to have:
     * {@code growCrops} writes the LOWER's age whether or not the segments above can be placed,
     * and unlike the random tick it has no light check to incidentally stop it.
     */
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return isLower(state) && this.getAge(state) < this.maxAgeFor(world, pos);
    }

    /**
     * The highest age this plant may grow to at {@code pos} given what is above it. The plant needs
     * one free block from {@link #TWO_TALL_AGE} and two from {@link #THREE_TALL_AGE}, so it stops
     * one stage short of whichever it cannot reach and waits there. Both ceilings matter: a plant
     * with two blocks of headroom is stopped at 5, not 3.
     *
     * <p>Holding the <em>age</em> back is what keeps the plant honest, rather than letting the age
     * run ahead and reconciling the shape afterwards: age is what the loot table and the models
     * read, so an age-7 lone stalk would look and harvest like a finished three-tall plant.
     */
    private int maxAgeFor(LevelReader world, BlockPos pos) {
        if (!this.canOccupy(world, pos.above())) {
            return TWO_TALL_AGE - 1;
        }
        if (!this.canOccupy(world, pos.above(2))) {
            return THREE_TALL_AGE - 1;
        }
        return this.getMaxAge();
    }

    /**
     * The plant's age as seen from this block. The canonical age lives on the LOWER segment, and
     * every segment above it reports {@link #getMaxAge()} — "nothing left to grow here", which is
     * the truth, since the LOWER drives the whole plant.
     *
     * <p>That is not cosmetic, it is what makes the plant safe around <b>bees</b>.
     * {@code Bee.BeeGrowCropGoal} looks 1–2 blocks below itself and, on anything in
     * {@code #minecraft:bee_growables} that is a {@code CropBlock}, does
     * {@code if (!isMaxAge(state)) setBlockAndUpdate(pos, getStateForAge(getAge(state) + 1))}. That write is
     * direct — it never goes through {@code growCrops} — and {@link CropBlock#getStateForAge} rebuilds
     * the state from {@code defaultBlockState()}, which resets {@link #SEGMENT} to LOWER. A bee
     * hovering over a tall plant would turn its MIDDLE or UPPER into a LOWER in mid-air, fail
     * {@code canSurvive}, and decapitate the plant.
     *
     * <p>The guard has to be {@code isMaxAge}, and {@code CropBlock#isMaxAge} is {@code final} —
     * but it is defined as {@code getAge(state) >= getMaxAge()}, so overriding {@code getAge} is
     * how a segment is made to report itself mature. Bees then skip everything but the LOWER,
     * where {@code getStateForAge} happens to produce exactly the right state.
     *
     * <p>Safe because nothing reads the age of a non-LOWER segment: the outline shape, growth,
     * fertilization and harvesting all either run on the LOWER only or go through {@link #SEGMENT},
     * and the loot table and blockstate read the {@link #AGE} property directly rather than this.
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
        if (age < this.getMaxAge() && world.getRawBrightness(pos, 0) >= 9) {
            // The ground answers for itself when it is a bed of ours (a pot or a tray): its own
            // moisture, and no crowding penalty, because the plant has soil or solution to itself.
            BlockState floor = world.getBlockState(pos.below());
            float moisture;
            float bed = 1.0F;
            if (floor.getBlock() instanceof GrowMedium medium) {
                moisture = medium.moisture(floor);
                bed = medium.speed(floor);
            } else {
                moisture = Services.PLATFORM.cropGrowthSpeed(state, world, pos);
            }
            if (HempGrowth.rolls(random, GROWTH_RESISTANCE, moisture,
                    bed * state.getValue(GrowLight.PROPERTY).speedAt(age))) {
                grown++;
            }
        }
        // Reconcile whether or not the plant just aged: this is the one place that repairs a plant
        // whose height fell behind its age — because it was boxed in earlier and now has the
        // headroom, or because a bee bumped the LOWER's age with a raw setBlockAndUpdate that never
        // went through setAge.
        //
        // Guarded on there being something to write, though. isRandomlyTicking is not gated on
        // maturity, so a finished field — the normal end state of a hemp farm — kept calling setAge
        // for ever: three Level#setBlock calls plus a maxAgeFor probe per tick per plant. The
        // writes themselves did nothing (an identical state early-outs in LevelChunk#setBlockState)
        // but each still paid a chunk lookup. isShapeSettled answers the same question in one or
        // two block reads.
        if (grown != age || !this.isShapeSettled(world, pos, age, state.getValue(GrowLight.PROPERTY))) {
            this.setAge(world, pos, grown, 0.0F, random);
        }
    }

    /**
     * Whether the plant at {@code pos} already carries the segments its age calls for, so a
     * reconciling {@link #setAge} at the same age would write nothing above the LOWER.
     *
     * <p>Deliberately built from {@link #stateFor} and the same {@link #canOccupy} tests
     * {@link #setAge} branches on, rather than restating the height rule: this has to mean
     * <em>exactly</em> "setAge would be a no-op", and two separately-worded versions of that would
     * drift the first time the height table changed. (The LOWER needs no check — setAge writes it
     * back through {@link Defoliation#carryOver}, which at an unchanged age reproduces the state
     * that is already there.) {@code light} is the LOWER's record, which every segment above copies.
     */
    private boolean isShapeSettled(LevelReader world, BlockPos pos, int age, GrowLight light) {
        if (age < TWO_TALL_AGE) {
            return true; // a single stalk: nothing above is ever written this young
        }
        BlockState mid = world.getBlockState(pos.above());
        if (!canOccupy(mid)) {
            return true; // boxed in at the second block, so setAge writes nothing above either
        }
        if (age >= THREE_TALL_AGE) {
            BlockState top = world.getBlockState(pos.above(2));
            if (canOccupy(top)) {
                return mid == this.stateFor(age, TriplePlantSegment.MIDDLE, light)
                        && top == this.stateFor(age, TriplePlantSegment.UPPER, light);
            }
        }
        // Two tall — either because that is all the age calls for, or because the third block is
        // blocked and setAge falls back to the two-tall branch.
        return mid == this.stateFor(age, TriplePlantSegment.UPPER, light);
    }

    // Bonemeal path: CropBlock.performBonemeal() calls growCrops() on the targeted block. Only the LOWER
    // segment is fertilizable, so this only ever runs on the LOWER segment. grow() is overridden as
    // well so the roll uses the random it is handed, which is also what lets a test decide the roll.
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

    /** A flat one stage per successful bonemeal, for the same reasons as {@link IndicaCropBlock}. */
    @Override
    protected int getBonemealAgeIncrease(Level world) {
        return 1;
    }

    /**
     * Whether a bonemeal application actually grows the plant. Bonemeal is consumed either way
     * (see {@code BoneMealItem.useOnFertilizable}), so a false here is a wasted application.
     */
    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextInt(BONEMEAL_FAILURE_CHANCE) != 0;
    }

    /**
     * Sets the LOWER segment to {@code newAge} and reconciles the segments above it with the height
     * that age calls for, as far as there is room. Because this runs on every growth step rather
     * than only at the exact age the plant gains a block, a plant that was boxed in catches up by
     * itself once the space is cleared.
     *
     * <p>{@code newAge} is clamped to {@link #maxAgeFor}, so a plant that cannot reach its full
     * height stalls one stage short of needing the blocked space rather than ageing on stunted.
     *
     * <p>The clamp deliberately never lowers an age the plant already has: a finished plant that a
     * player later builds over keeps its age and its harvest instead of silently reverting.
     */
    private void setAge(Level world, BlockPos pos, int newAge, float extraStress, RandomSource random) {
        BlockState current = world.getBlockState(pos);
        int currentAge = current.is(this) && isLower(current) ? this.getAge(current) : 0;
        newAge = Math.min(newAge, this.getMaxAge());
        newAge = Math.min(newAge, Math.max(currentAge, this.maxAgeFor(world, pos)));

        // stateFor rebuilds from defaultBlockState(), so the LOWER's trim flags have to be carried
        // across explicitly or every growth step would quietly wipe them — and randomTick calls
        // this on every tick, not only when the plant actually ages.
        BlockState lower = Defoliation.carryOver(current, this.stateFor(newAge, TriplePlantSegment.LOWER));
        if (newAge > currentAge && current.is(this)) {
            lower = lower.setValue(GrowLight.PROPERTY, PlantStress.afterStep(world, pos,
                    current.getValue(GrowLight.PROPERTY), currentAge, newAge, 3, extraStress, random));
            if (newAge == this.getMaxAge()) {
                BlockPos floorPos = pos.below();
                BlockState floor = world.getBlockState(floorPos);
                if (floor.getBlock() instanceof GrowMedium medium) {
                    medium.spend(world, floorPos, floor);
                }
            }
        }
        world.setBlock(pos, lower, Block.UPDATE_CLIENTS);

        BlockPos midPos = pos.above();
        BlockPos topPos = pos.above(2);
        boolean midFree = this.canOccupy(world, midPos);
        GrowLight light = lower.getValue(GrowLight.PROPERTY);

        if (newAge >= THREE_TALL_AGE && midFree && this.canOccupy(world, topPos)) {
            // Bottom-up: the new UPPER needs the MIDDLE under it to already be in place, otherwise
            // its canSurvive fails and the neighbour update wipes it straight back out.
            world.setBlock(midPos, this.stateFor(newAge, TriplePlantSegment.MIDDLE, light), Block.UPDATE_CLIENTS);
            world.setBlock(topPos, this.stateFor(newAge, TriplePlantSegment.UPPER, light), Block.UPDATE_CLIENTS);
        } else if (newAge >= TWO_TALL_AGE && midFree) {
            world.setBlock(midPos, this.stateFor(newAge, TriplePlantSegment.UPPER, light), Block.UPDATE_CLIENTS);
        }
    }

    /** Whether the plant may grow into {@code pos} — empty space, or a segment it already owns. */
    private boolean canOccupy(LevelReader world, BlockPos pos) {
        return canOccupy(world.getBlockState(pos));
    }

    /** As above, for a state already in hand — {@link #isShapeSettled} reads each block once. */
    private boolean canOccupy(BlockState state) {
        return state.isAir() || (state.is(this) && !isLower(state));
    }

    // ----- defoliation -----

    /**
     * Shearing a growing plant takes a fan leaf and shifts its eventual harvest towards buds, the
     * same as on {@link IndicaCropBlock}; the logic lives in {@link Defoliation}. Any of the three
     * segments can be clicked — {@link #findLowerPos} resolves to the LOWER, which is where the age
     * and the trim flags are kept.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos lowerPos = this.findLowerPos(world, pos, state);
        BlockState lower = world.getBlockState(lowerPos);
        if (lower.is(this) && isLower(lower)) {
            // A bottle on the plant waters its pot: that is where a player aims.
            ItemInteractionResult watered = GrowPotBlock.tryWater(stack, world, lowerPos.below(), player, hand);
            if (watered != null) {
                return watered;
            }
            ItemInteractionResult result = Defoliation.tryCut(world, lowerPos, lower, lower.getValue(AGE),
                    stack, player, hand);
            if (result != null) {
                return result;
            }
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    // ----- the smell of a ripe plant -----

    /**
     * A ripe plant lets off a wisp now and then from its top; see {@link RipeAroma}. Any segment
     * can be sampled, and the helper keeps only the one with open air above it.
     *
     * <p>Reads {@link #AGE} directly, <b>not</b> {@link #getAge}: every segment above the LOWER
     * reports maturity through {@code getAge} (the bee guard above), so asking it would have every
     * tall plant smelling ripe from age 4. Their {@code AGE} mirrors the LOWER's, so any segment
     * answers truthfully here.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= this.getMaxAge()) {
            RipeAroma.randomDisplayTick(world, pos, random);
        }
    }

    // ----- harvesting -----
    // Breaking any segment takes the whole plant and yields it exactly once: the loot table only
    // drops for the LOWER segment, so we drop the LOWER's loot by hand when the player broke one
    // of the others, and clear the rest of the stack top-down with drops suppressed.
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide) {
            BlockPos lowerPos = this.findLowerPos(world, pos, state);
            BlockState lower = world.getBlockState(lowerPos);
            if (lower.is(this) && isLower(lower)) {
                // The trim flags and the canonical age both live on the LOWER, so the harvest
                // criterion is fed from there whichever segment the player actually broke.
                if (this.isMaxAge(lower)) {
                    HarvestHempCriterion.trigger(player, (ServerLevel) world, lowerPos, lower);
                }
                for (int offset = 2; offset >= 1; offset--) {
                    BlockPos segmentPos = lowerPos.above(offset);
                    BlockState segment = world.getBlockState(segmentPos);
                    if (!segmentPos.equals(pos) && segment.is(this) && !isLower(segment)) {
                        world.setBlock(segmentPos, Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
                if (!lowerPos.equals(pos)) {
                    if (!player.isCreative()) {
                        dropResources(lower, world, lowerPos, null, player, player.getMainHandItem());
                    }
                    world.setBlock(lowerPos, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    /**
     * Where the LOWER segment of the plant {@code pos} belongs to sits. An UPPER is one block up
     * while the plant is two tall and two blocks up once it is three tall, so that case is
     * resolved by looking at what is underneath it.
     */
    private BlockPos findLowerPos(LevelReader world, BlockPos pos, BlockState state) {
        return switch (state.getValue(SEGMENT)) {
            case LOWER -> pos;
            case MIDDLE -> pos.below();
            case UPPER -> {
                BlockState below = world.getBlockState(pos.below());
                yield below.is(this) && below.getValue(SEGMENT) == TriplePlantSegment.MIDDLE
                        ? pos.below(2)
                        : pos.below();
            }
        };
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SEGMENT, Defoliation.TRIMMED_EARLY, Defoliation.TRIMMED_LATE, Defoliation.RUBBED,
                GrowLight.PROPERTY);
    }
}
