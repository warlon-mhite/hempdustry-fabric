package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.strain.Chemotype;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A block made of nine pieces of one stamped hemp matter, which keeps their chemotype as its
 * {@link ChemotypeBlocks#CHEMOTYPE} state: the Block of Hemp Leaves and the charas ball.
 */
public class StampedBlock extends Block {

    public StampedBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ChemotypeBlocks.CHEMOTYPE, Chemotype.THC_DOMINANT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ChemotypeBlocks.CHEMOTYPE);
    }
}
