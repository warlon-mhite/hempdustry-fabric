package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * A two-tall crop. Redesigned (approach B, see CLAUDE.md §8) around the vanilla
 * double-block pattern: a {@link #HALF} property (LOWER/UPPER) plus the standard
 * {@code age 0..7}. The canonical age lives on the LOWER half; the UPPER half
 * mirrors it. The plant is planted as a single LOWER stalk and grows into its
 * two-tall form once the lower half reaches {@link #DOUBLE_BLOCK_AGE}.
 */
public class IndicaCropBlock extends CropBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final IntProperty AGE = Properties.AGE_7;
    /** Highest age value; a lower half at this age is mature. Matches {@code Properties.AGE_7}. */
    public static final int MAX_AGE = 7;

    /** The lower half sprouts an upper half once it reaches this age (and there is room above). */
    public static final int DOUBLE_BLOCK_AGE = 4;

    /** One in this many bonemeal applications does nothing (but is still consumed). */
    public static final int BONEMEAL_FAILURE_CHANCE = 3;

    private static final VoxelShape[] LOWER_SHAPE_BY_AGE = new VoxelShape[]{
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    private static final VoxelShape UPPER_SHAPE =
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public IndicaCropBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(this.getAgeProperty(), 0)
                .with(HALF, DoubleBlockHalf.LOWER));
    }

    private static boolean isLower(BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER;
    }

    private BlockState stateFor(int age, DoubleBlockHalf half) {
        return this.getDefaultState().with(this.getAgeProperty(), age).with(HALF, half);
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.INDICA_SEEDS;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            return UPPER_SHAPE;
        }
        return LOWER_SHAPE_BY_AGE[Math.min(this.getAge(state), LOWER_SHAPE_BY_AGE.length - 1)];
    }

    // ----- support / placement -----
    // The UPPER half only survives directly above a LOWER half of this block. The
    // LOWER half uses the standard crop rule (enough light + farmland below), which
    // also prevents seeds from being placed onto an existing plant. PlantBlock's
    // getStateForNeighborUpdate (inherited) breaks either half as soon as canPlaceAt
    // fails, so removing the lower half cascades to the upper.
    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = world.getBlockState(pos.down());
            return below.isOf(this) && below.get(HALF) == DoubleBlockHalf.LOWER;
        }
        return super.canPlaceAt(state, world, pos);
    }

    // ----- growth -----
    // Only the lower half is fertile and random-ticks; it drives the whole plant.
    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return isLower(state) && this.getAge(state) < this.getMaxAge();
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return isLower(state) && !this.isMature(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!isLower(state)) {
            return;
        }
        if (world.getBaseLightLevel(pos, 0) >= 9) {
            int age = this.getAge(state);
            if (age < this.getMaxAge()) {
                float moisture = getAvailableMoisture(this, world, pos);
                if (random.nextInt((int) (25.0F / moisture) + 1) == 0) {
                    this.setAge(world, pos, age + 1);
                }
            }
        }
    }

    // Bonemeal path: CropBlock.grow() calls applyGrowth() on the targeted block.
    // Only the lower half is fertilizable, so this only ever runs on the lower half.
    @Override
    public void applyGrowth(World world, BlockPos pos, BlockState state) {
        if (!isLower(state)) {
            return;
        }
        this.setAge(world, pos, this.getAge(state) + this.getGrowthAmount(world));
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
    protected int getGrowthAmount(World world) {
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
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return random.nextInt(BONEMEAL_FAILURE_CHANCE) != 0;
    }

    /**
     * Sets the lower half to {@code newAge} (clamped) and keeps the upper half in
     * sync: it mirrors the age when present, and is sprouted once the plant reaches
     * {@link #DOUBLE_BLOCK_AGE} and there is room above. If the space above is
     * occupied the plant simply stays single until it grows again with room free.
     */
    private void setAge(World world, BlockPos pos, int newAge) {
        newAge = Math.min(newAge, this.getMaxAge());
        world.setBlockState(pos, this.stateFor(newAge, DoubleBlockHalf.LOWER), Block.NOTIFY_LISTENERS);

        BlockPos upPos = pos.up();
        BlockState above = world.getBlockState(upPos);
        boolean upperPresent = above.isOf(this) && above.get(HALF) == DoubleBlockHalf.UPPER;
        if (upperPresent) {
            world.setBlockState(upPos, this.stateFor(newAge, DoubleBlockHalf.UPPER), Block.NOTIFY_LISTENERS);
        } else if (newAge >= DOUBLE_BLOCK_AGE && above.isAir()) {
            world.setBlockState(upPos, this.stateFor(newAge, DoubleBlockHalf.UPPER), Block.NOTIFY_LISTENERS);
        }
    }

    // ----- harvesting -----
    // Breaking either half removes both and yields the plant exactly once: the loot
    // table only drops for the lower half, so we drop the lower's loot (when an upper
    // half is broken directly) and remove the partner without letting it drop.
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            if (state.get(HALF) == DoubleBlockHalf.UPPER) {
                BlockPos lowerPos = pos.down();
                BlockState lower = world.getBlockState(lowerPos);
                if (lower.isOf(this) && lower.get(HALF) == DoubleBlockHalf.LOWER) {
                    if (!player.isCreative()) {
                        dropStacks(lower, world, lowerPos, null, player, player.getMainHandStack());
                    }
                    world.setBlockState(lowerPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                }
            } else {
                BlockPos upperPos = pos.up();
                BlockState upper = world.getBlockState(upperPos);
                if (upper.isOf(this) && upper.get(HALF) == DoubleBlockHalf.UPPER) {
                    world.setBlockState(upperPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected IntProperty getAgeProperty() {
        return AGE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF);
    }
}
