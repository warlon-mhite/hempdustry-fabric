package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
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
    public static final int GROW_UP_STAGE = 3;
    public static final int SECOND_STAGE_MAX_AGE = 11;
    private static final VoxelShape[] AGE_TO_SHAPE =
            new VoxelShape[]{
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D),
                    Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
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
                world.getBlockState(pos.down(1)).get(AGE) >= GROW_UP_STAGE && world.getBlockState(pos.down(1)).get(AGE) <= FIRST_STAGE_MAX_AGE);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBaseLightLevel(pos, 0) >= 9) {
            int currentAge = this.getAge(state);

            if (currentAge < FIRST_STAGE_MAX_AGE) {
                float growthChance = getAvailableMoisture(this, world, pos);
                if (random.nextInt((int) (25.0F / growthChance) + 1) == 0) {
                    if (currentAge == GROW_UP_STAGE && world.getBlockState(pos.up(1)).isAir()) {
                        world.setBlockState(pos, this.withAge(currentAge + 1), 2);
                        world.setBlockState(pos.up(1), this.withAge(FIRST_STAGE_MAX_AGE + 1), 2);
                    }
                    else if (currentAge > GROW_UP_STAGE && world.getBlockState(pos.up(1)).isOf(this)) {
                        BlockState blockStateAbove = world.getBlockState(pos.up(1));
                        int blockAboveAge = blockStateAbove.get(AGE);
                        world.setBlockState(pos, this.withAge(currentAge + 1), 2);
                        world.setBlockState(pos.up(1), this.withAge(blockAboveAge + 1), 2);
                    }
                    else if (currentAge >= GROW_UP_STAGE &&  !world.getBlockState(pos.up(1)).isAir() && !world.getBlockState(pos.up(1)).isOf(this)){
                        world.setBlockState(pos, this.withAge(currentAge),2);
                    }
                    else {
                        world.setBlockState(pos, this.withAge(currentAge + 1), 2);
                    }
                }
            }
        }
    }
    @Override
    public void applyGrowth(World world, BlockPos pos, BlockState state) {
        int nextAge = this.getAge(state) + this.getGrowthAmount(world);
        int maxAge = this.getMaxAge();
        if (nextAge > maxAge) {
            nextAge = maxAge;
        }
        int currentAge = this.getAge(state);

        if(this.getAge(state) == GROW_UP_STAGE && world.getBlockState(pos.up(1)).isOf(Blocks.AIR)) {
            world.setBlockState(pos.up(1), this.withAge(FIRST_STAGE_MAX_AGE + 1), 2);
        }
        if(this.getAge(state) > GROW_UP_STAGE && this.getAge(state) <= SECOND_STAGE_MAX_AGE && world.getBlockState(pos.up(1)).isOf(this)) {
            BlockState blockStateAbove = world.getBlockState(pos.up(1));
            int blockAboveAge = blockStateAbove.get(AGE);
            world.setBlockState(pos.up(1), this.withAge(blockAboveAge + 1), 2);
        }
        else {
            world.setBlockState(pos, this.withAge(nextAge - SECOND_STAGE_MAX_AGE), 2);
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        int currentAge = this.getAge(state);
        if (!world.isClient() && (currentAge > FIRST_STAGE_MAX_AGE)) {
            world.breakBlock(pos.down(1), false);
        }
        super.onBreak(world, pos, state, player);
        return state;
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