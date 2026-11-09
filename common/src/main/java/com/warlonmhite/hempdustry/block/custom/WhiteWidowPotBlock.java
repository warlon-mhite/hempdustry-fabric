package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Potted Wild White Widow, which keeps the flower's hours in its pot, as a potted eyeblossom does.
 * Vanilla's potted eyeblossoms are two blocks swapped for each other; this is one block with the
 * flower's {@link WhiteWidowFlowerBlock#OPEN}, so the pot drops and picks back as one thing.
 */
public class WhiteWidowPotBlock extends FlowerPotBlock {

    public WhiteWidowPotBlock(Block flower, Properties settings) {
        super(flower, settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(WhiteWidowFlowerBlock.OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WhiteWidowFlowerBlock.OPEN);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        WhiteWidowFlowerBlock.turn(state, world, pos);
    }
}
