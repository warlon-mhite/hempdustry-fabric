package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

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
            EnumProperty.of("segment", TriplePlantSegment.class);
    public static final IntProperty AGE = Properties.AGE_7;
    /** Highest age value; a LOWER segment at this age is mature. Matches {@code Properties.AGE_7}. */
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
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 5.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 11.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    private static final VoxelShape MIDDLE_SHAPE =
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape UPPER_SHAPE =
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D);

    public SativaCropBlock(Settings settings) {
        super(settings);
        // Defoliation.untrimmed is load-bearing: a BooleanProperty left out of the default state
        // resolves to *true*, which would plant every seed pre-trimmed. See its javadoc.
        this.setDefaultState(Defoliation.untrimmed(this.stateManager.getDefaultState()
                .with(this.getAgeProperty(), 0)
                .with(SEGMENT, TriplePlantSegment.LOWER)));
    }

    private static boolean isLower(BlockState state) {
        return state.get(SEGMENT) == TriplePlantSegment.LOWER;
    }

    private BlockState stateFor(int age, TriplePlantSegment segment) {
        return this.getDefaultState().with(this.getAgeProperty(), age).with(SEGMENT, segment);
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.SATIVA_SEEDS;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(SEGMENT)) {
            case UPPER -> UPPER_SHAPE;
            case MIDDLE -> MIDDLE_SHAPE;
            case LOWER -> LOWER_SHAPE_BY_AGE[Math.min(this.getAge(state), LOWER_SHAPE_BY_AGE.length - 1)];
        };
    }

    // ----- support / placement -----
    // Every segment above the bottom only survives on top of the right partner, so removing any
    // one of them makes the ones above it unsupported. PlantBlock's getStateForNeighborUpdate
    // (inherited) turns an unsupported segment into air, so the removal cascades upwards on its
    // own. The LOWER segment uses the standard crop rule (enough light + farmland below), which
    // is also what stops a seed being planted onto an existing plant.
    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        return switch (state.get(SEGMENT)) {
            // The UPPER sits on the LOWER while the plant is two tall, and on the MIDDLE once
            // it is three tall, so it accepts either.
            case UPPER -> below.isOf(this) && below.get(SEGMENT) != TriplePlantSegment.UPPER;
            case MIDDLE -> below.isOf(this) && below.get(SEGMENT) == TriplePlantSegment.LOWER;
            case LOWER -> super.canPlaceAt(state, world, pos);
        };
    }

    // ----- growth -----
    // Only the LOWER segment is fertile and random-ticks; it drives the whole plant.
    @Override
    protected boolean hasRandomTicks(BlockState state) {
        // Deliberately *not* gated on `age < maxAge` the way vanilla's crops (and indica) are. A
        // plant that hit age 7 while something was sitting two blocks above it is mature but a
        // block short, and this tick is what lets it finish once the space is cleared instead of
        // staying stunted forever. The extra ticks only ever cost two block-state reads (see the
        // mature branch of randomTick below).
        return isLower(state);
    }

    /**
     * Bonemeal is refused — and so not consumed — once the plant has grown as far as its headroom
     * allows. Without the {@link #maxAgeFor} half of this test, bonemeal would push a plant that is
     * boxed in all the way to maturity at whatever stunted height it happens to have:
     * {@code applyGrowth} writes the LOWER's age whether or not the segments above can be placed,
     * and unlike the random tick it has no light check to incidentally stop it.
     */
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
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
    private int maxAgeFor(WorldView world, BlockPos pos) {
        if (!this.canOccupy(world, pos.up())) {
            return TWO_TALL_AGE - 1;
        }
        if (!this.canOccupy(world, pos.up(2))) {
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
     * {@code BeeEntity.GrowCropsGoal} looks 1–2 blocks below itself and, on anything in
     * {@code #minecraft:bee_growables} that is a {@code CropBlock}, does
     * {@code if (!isMature(state)) setBlockState(pos, withAge(getAge(state) + 1))}. That write is
     * direct — it never goes through {@code applyGrowth} — and {@link CropBlock#withAge} rebuilds
     * the state from {@code getDefaultState()}, which resets {@link #SEGMENT} to LOWER. A bee
     * hovering over a tall plant would turn its MIDDLE or UPPER into a LOWER in mid-air, fail
     * {@code canPlaceAt}, and decapitate the plant.
     *
     * <p>The guard has to be {@code isMature}, and {@code CropBlock#isMature} is {@code final} —
     * but it is defined as {@code getAge(state) >= getMaxAge()}, so overriding {@code getAge} is
     * how a segment is made to report itself mature. Bees then skip everything but the LOWER,
     * where {@code withAge} happens to produce exactly the right state.
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
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!isLower(state)) {
            return;
        }
        int age = this.getAge(state);
        if (age < this.getMaxAge() && world.getBaseLightLevel(pos, 0) >= 9) {
            float moisture = getAvailableMoisture(this, world, pos);
            if (random.nextInt((int) (GROWTH_RESISTANCE / moisture) + 1) == 0) {
                age++;
            }
        }
        // Always reconcile, whether or not the plant just aged: this is the one place that repairs
        // a plant whose height fell behind its age — because it was boxed in earlier and now has
        // the headroom, or because a bee bumped the LOWER's age with a raw setBlockState that
        // never went through setAge. Writing a block its own current state is a no-op in
        // World#setBlockState, so a plant that is already correct costs nothing here.
        this.setAge(world, pos, age);
    }

    // Bonemeal path: CropBlock.grow() calls applyGrowth() on the targeted block. Only the LOWER
    // segment is fertilizable, so this only ever runs on the LOWER segment.
    @Override
    public void applyGrowth(World world, BlockPos pos, BlockState state) {
        if (!isLower(state)) {
            return;
        }
        this.setAge(world, pos, this.getAge(state) + this.getGrowthAmount(world));
    }

    /** A flat one stage per successful bonemeal, for the same reasons as {@link IndicaCropBlock}. */
    @Override
    protected int getGrowthAmount(World world) {
        return 1;
    }

    /**
     * Whether a bonemeal application actually grows the plant. Bonemeal is consumed either way
     * (see {@code BoneMealItem.useOnFertilizable}), so a false here is a wasted application.
     */
    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
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
    private void setAge(World world, BlockPos pos, int newAge) {
        BlockState current = world.getBlockState(pos);
        int currentAge = current.isOf(this) && isLower(current) ? this.getAge(current) : 0;
        newAge = Math.min(newAge, this.getMaxAge());
        newAge = Math.min(newAge, Math.max(currentAge, this.maxAgeFor(world, pos)));

        // stateFor rebuilds from getDefaultState(), so the LOWER's trim flags have to be carried
        // across explicitly or every growth step would quietly wipe them — and randomTick calls
        // this on every tick, not only when the plant actually ages.
        BlockState lower = Defoliation.carryOver(current, this.stateFor(newAge, TriplePlantSegment.LOWER));
        world.setBlockState(pos, lower, Block.NOTIFY_LISTENERS);

        BlockPos midPos = pos.up();
        BlockPos topPos = pos.up(2);
        boolean midFree = this.canOccupy(world, midPos);

        if (newAge >= THREE_TALL_AGE && midFree && this.canOccupy(world, topPos)) {
            // Bottom-up: the new UPPER needs the MIDDLE under it to already be in place, otherwise
            // its canPlaceAt fails and the neighbour update wipes it straight back out.
            world.setBlockState(midPos, this.stateFor(newAge, TriplePlantSegment.MIDDLE), Block.NOTIFY_LISTENERS);
            world.setBlockState(topPos, this.stateFor(newAge, TriplePlantSegment.UPPER), Block.NOTIFY_LISTENERS);
        } else if (newAge >= TWO_TALL_AGE && midFree) {
            world.setBlockState(midPos, this.stateFor(newAge, TriplePlantSegment.UPPER), Block.NOTIFY_LISTENERS);
        }
    }

    /** Whether the plant may grow into {@code pos} — empty space, or a segment it already owns. */
    private boolean canOccupy(WorldView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || (state.isOf(this) && !isLower(state));
    }

    // ----- defoliation -----

    /**
     * Shearing a growing plant takes a fan leaf and shifts its eventual harvest towards buds, the
     * same as on {@link IndicaCropBlock}; the logic lives in {@link Defoliation}. Any of the three
     * segments can be clicked — {@link #findLowerPos} resolves to the LOWER, which is where the age
     * and the trim flags are kept.
     */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockPos lowerPos = this.findLowerPos(world, pos, state);
        BlockState lower = world.getBlockState(lowerPos);
        if (lower.isOf(this) && isLower(lower)) {
            ItemActionResult result = Defoliation.tryCut(world, lowerPos, lower, lower.get(AGE),
                    stack, player, hand);
            if (result != null) {
                return result;
            }
        }
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    // ----- harvesting -----
    // Breaking any segment takes the whole plant and yields it exactly once: the loot table only
    // drops for the LOWER segment, so we drop the LOWER's loot by hand when the player broke one
    // of the others, and clear the rest of the stack top-down with drops suppressed.
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            BlockPos lowerPos = this.findLowerPos(world, pos, state);
            BlockState lower = world.getBlockState(lowerPos);
            if (lower.isOf(this) && isLower(lower)) {
                for (int offset = 2; offset >= 1; offset--) {
                    BlockPos segmentPos = lowerPos.up(offset);
                    BlockState segment = world.getBlockState(segmentPos);
                    if (!segmentPos.equals(pos) && segment.isOf(this) && !isLower(segment)) {
                        world.setBlockState(segmentPos, Blocks.AIR.getDefaultState(),
                                Block.NOTIFY_ALL | Block.SKIP_DROPS);
                    }
                }
                if (!lowerPos.equals(pos)) {
                    if (!player.isCreative()) {
                        dropStacks(lower, world, lowerPos, null, player, player.getMainHandStack());
                    }
                    world.setBlockState(lowerPos, Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL | Block.SKIP_DROPS);
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    /**
     * Where the LOWER segment of the plant {@code pos} belongs to sits. An UPPER is one block up
     * while the plant is two tall and two blocks up once it is three tall, so that case is
     * resolved by looking at what is underneath it.
     */
    private BlockPos findLowerPos(WorldView world, BlockPos pos, BlockState state) {
        return switch (state.get(SEGMENT)) {
            case LOWER -> pos;
            case MIDDLE -> pos.down();
            case UPPER -> {
                BlockState below = world.getBlockState(pos.down());
                yield below.isOf(this) && below.get(SEGMENT) == TriplePlantSegment.MIDDLE
                        ? pos.down(2)
                        : pos.down();
            }
        };
    }

    @Override
    protected IntProperty getAgeProperty() {
        return AGE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE, SEGMENT, Defoliation.TRIMMED_EARLY, Defoliation.TRIMMED_LATE);
    }
}
