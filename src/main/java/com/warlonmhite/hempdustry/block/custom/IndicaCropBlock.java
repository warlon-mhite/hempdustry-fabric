package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.*;
import net.minecraft.item.ItemConvertible;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class IndicaCropBlock extends CropBlock {

    public static final int FIRST_STAGE_MAX_AGE = 7;
    public static final int SECOND_STAGE_MAX_AGE = 3;
    private static final VoxelShape[] AGE_TO_SHAPE =
            new VoxelShape[]{
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 5.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D)};

    public static final IntProperty AGE = IntProperty.of("age", 0, 11);
    public IndicaCropBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return AGE_TO_SHAPE[this.getAge(state)];
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return super.canPlaceAt(state, world, pos) || (world.getBlockState(pos.down(1)).isOf(this) &&
                world.getBlockState(pos.down(1)).get(AGE) >= 4 && world.getBlockState(pos.down(1)).get(AGE) <= 7);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBaseLightLevel(pos, 0) >= 9) {
            int currentAge = this.getAge(state);
            if (currentAge < this.getMaxAge()) {
                float growthChance = getAvailableMoisture(this, world, pos);
                if (random.nextInt((int)(25.0F / growthChance) + 1) == 0) {
                    grow(world, pos, state, currentAge);
                }
            }
        }
    }

    private void grow(World world, BlockPos pos, BlockState state, int currentAge) {
        if (currentAge < FIRST_STAGE_MAX_AGE) {
            world.setBlockState(pos, this.withAge(currentAge + 1), 2);

            if (currentAge == 4 && world.getBlockState(pos.up()).isAir()) {
                world.setBlockState(pos.up(), this.withAge(FIRST_STAGE_MAX_AGE + 1), 2);
            }
        } else if (currentAge > FIRST_STAGE_MAX_AGE) {
            // Grow second block (synchronized stages)
            int secondBlockAge = currentAge - FIRST_STAGE_MAX_AGE; // Age difference between first and second block
            if (world.getBlockState(pos.down()).isOf(this) && world.getBlockState(pos.down(1)).get(AGE) < 7) {
                // Update both blocks together (first and second)
                world.setBlockState(pos.down(),this.withAge(currentAge + 1), 2);
                world.setBlockState(pos, this.withAge(FIRST_STAGE_MAX_AGE + secondBlockAge), 2); // Grow the first block
            }
             // Grow the second block
        }
    }

    public void applyGrowth(World world, BlockPos pos, BlockState state) {
        int currentAge = this.getAge(state);
        int growthAmount = this.getGrowthAmount(world);
        int newAge = Math.min(currentAge + growthAmount, this.getMaxAge());

        grow(world, pos, state, currentAge);
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.INDICA_SEEDS;
    }

    @Override
    public int getMaxAge() {
        return 11;
    }
    @Override
    protected IntProperty getAgeProperty() {
        return AGE;
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}