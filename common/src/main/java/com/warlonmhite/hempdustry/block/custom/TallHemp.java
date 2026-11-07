package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.function.BooleanSupplier;

/** What every tall hemp plant shares, whichever crop it is. */
public final class TallHemp {

    /**
     * How deep we are inside one of our own writes to a plant, per thread, since every level's writes run on
     * the thread that ticks it: a mod ticking dimensions in parallel must not let one level's own write hide
     * another's raw one. Server side only, like {@code onPlace}, which reads it. An {@code int[]} so a write
     * boxes nothing.
     */
    private static final ThreadLocal<int[]> OWN_WRITES = ThreadLocal.withInitial(() -> new int[1]);

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

    /**
     * Runs a write to a plant that already went through the plant's own rules (its growth step, or the bee's),
     * so that the crop's {@code onPlace} leaves it alone.
     */
    public static boolean ownWrite(BooleanSupplier write) {
        int[] depth = OWN_WRITES.get();
        depth[0]++;
        try {
            return write.getAsBoolean();
        } finally {
            depth[0]--;
        }
    }

    /**
     * Whether {@code state}, just written over {@code old}, came from outside the plant's own rules: another
     * mod's harvester, fertiliser, soil or bee writing {@code crop.getStateForAge(age)}. That state is the
     * crop's default with a new age, so it carries no trims, no rub and a natural record, and it is a root
     * wherever it lands.
     *
     * <p>Only that exact state counts. Every other write of ours either sets something (a trim, a rub, the
     * stressed record) or runs inside {@link #ownWrite}; and a mod that writes {@code state.setValue(AGE, n)}
     * keeps everything else, so it has nothing to repair.
     */
    static boolean isRawWrite(CropBlock crop, BlockState state, BlockState old) {
        return OWN_WRITES.get()[0] == 0 && old.is(crop) && state == crop.getStateForAge(crop.getAge(state));
    }
}
