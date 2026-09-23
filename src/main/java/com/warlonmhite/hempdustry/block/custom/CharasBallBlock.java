package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Nine charas rolled into a ball. A plain block in every way but its size: the outline is the
 * ball's own 8x6x8 footprint, so the selection box and the collision hug a small lump on the
 * floor instead of a whole cube of air around it.
 */
public class CharasBallBlock extends Block {
    public static final MapCodec<CharasBallBlock> CODEC = createCodec(CharasBallBlock::new);

    // The widest extent of the three boxes in models/block/charas_ball.json.
    private static final VoxelShape SHAPE = Block.createCuboidShape(4, 0, 4, 12, 6, 12);

    public CharasBallBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends CharasBallBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
