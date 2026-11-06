package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** What every tall hemp plant shares, whichever crop it is. */
public final class TallHemp {

    private TallHemp() {
    }

    /**
     * Whether {@code state} is the block that drives a plant: the LOWER segment of a tall hemp crop, or any
     * state that is not one (a block of one segment is its own root). Growth, bone meal and loot all run on
     * the root; the blocks above only follow it.
     */
    public static boolean isRoot(BlockState state) {
        if (state.getBlock() instanceof SativaCropBlock) {
            return state.getValue(SativaCropBlock.SEGMENT) == TriplePlantSegment.LOWER;
        }
        if (state.getBlock() instanceof IndicaCropBlock) {
            return state.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }
}
