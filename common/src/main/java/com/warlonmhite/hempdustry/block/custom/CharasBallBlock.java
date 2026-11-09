package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Nine charas rolled into a ball. A plain block in every way but its size: the outline is the
 * ball's own 8x6x8 footprint, so the selection box and the collision hug a small lump on the
 * floor instead of a whole cube of air around it.
 */
public class CharasBallBlock extends StampedBlock {
    // The widest extent of the three boxes in models/block/charas_ball.json.
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 6, 12);

    public CharasBallBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
