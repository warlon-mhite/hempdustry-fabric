package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

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
     * Whether {@code state} is the block that drives a plant: the segment on the block a tall hemp crop grows
     * from (the LOWER of a standing plant, the UPPER of one that hangs from a ceiling), or any state that is
     * not one (a block of one segment is its own root). Growth, bone meal and loot all run on the root; the
     * other blocks only follow it.
     */
    public static boolean isRoot(BlockState state) {
        if (state.getBlock() instanceof SativaCropBlock) {
            return state.getValue(SativaCropBlock.SEGMENT) == TriplePlantSegment.LOWER;
        }
        if (state.getBlock() instanceof IndicaCropBlock indica) {
            return indica.isRoot(state);
        }
        if (state.getBlock() instanceof EnderHazeCropBlock) {
            return EnderHazeCropBlock.isRoot(state);
        }
        return true;
    }

    /** The way {@code crop} grows from its root: up from a floor, or down from a ceiling for a plant that hangs. */
    static Direction growth(Block crop) {
        return crop instanceof IndicaCropBlock indica ? indica.growth() : Direction.UP;
    }

    /**
     * Where the root of the plant that the block of {@code crop} at {@code pos} belongs to stands, walking back
     * towards the root through the segments that grow from it, or {@code null} when that block is not part of a
     * whole plant (a segment with no root beside it, about to break off). Vanilla's pitcher crop finds its lower
     * half the same way for bone meal ({@code PitcherCropBlock#getLowerHalf}).
     */
    static @Nullable BlockPos rootOf(Block crop, LevelReader world, BlockPos pos, BlockState state) {
        Direction towardsRoot = growth(crop).getOpposite();
        BlockPos at = pos;
        BlockState here = state;
        for (int step = 0; step < EnderHazeCropBlock.HEIGHT; step++) {
            if (!here.is(crop)) {
                return null;
            }
            if (isRoot(here)) {
                return at;
            }
            if (!here.canSurvive(world, at)) {
                return null;
            }
            at = at.relative(towardsRoot);
            here = world.getBlockState(at);
        }
        return null;
    }

    /**
     * Whether {@code state}, just written over {@code old} on a block above the root from outside the plant's own
     * rules, raises its age: an instant fertiliser clicked on a top writes {@code getStateForAge(max)} there, as
     * Mystical Agriculture's does, or sets the age alone. A plant's age is its root's, so the crop puts the block
     * back and grows the plant from its root to that age instead, as one step of its own.
     */
    static boolean isRaisedAboveTheRoot(CropBlock crop, BlockState state, BlockState old) {
        return !inOwnWrite() && old.is(crop) && state.is(crop) && !isRoot(old)
                && crop.getAge(state) > crop.getAge(old);
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
     * keeps everything else, which on the root is all a growth step keeps. A younger age written so on the root
     * is {@link #isSetBackByAge}; above the root it is {@link #isAgeWrittenAboveTheRoot}.
     */
    static boolean isRawWrite(CropBlock crop, BlockState state, BlockState old) {
        return !inOwnWrite() && old.is(crop) && state == crop.getStateForAge(crop.getAge(state));
    }

    /**
     * Whether {@code state}, just written over {@code old}, is another mod's change of age on a block above the
     * root that keeps everything else ({@code state.setValue(AGE, n)}): a right-click harvest that replants the
     * block clicked, for one. A plant's age is its root's and so is its loot, so such a write harvested nothing
     * and grew nothing, and the block goes back as it was. Our own steps set a segment's age only inside
     * {@link #ownWrite}.
     */
    static boolean isAgeWrittenAboveTheRoot(CropBlock crop, BlockState state, BlockState old) {
        return !inOwnWrite() && old.is(crop) && state.is(crop) && !isRoot(old)
                && crop.getAge(state) != crop.getAge(old);
    }

    /**
     * Whether {@code state}, just written over {@code old}, sets the root back to a younger age from outside the
     * plant's own steps, keeping everything else: a right-click harvest that replants, or {@code /setblock}.
     * The crop then keeps only the flags the new age could have earned ({@link Defoliation#onlyEarned}). Our
     * own steps lower no age outside {@link #ownWrite}.
     */
    static boolean isSetBackByAge(CropBlock crop, BlockState state, BlockState old) {
        return !inOwnWrite() && old.is(crop) && state.is(crop) && isRoot(old) && isRoot(state)
                && crop.getAge(state) < crop.getAge(old);
    }

    /** Whether one of the plant's own writes ({@link #ownWrite}) is running on this thread. */
    static boolean inOwnWrite() {
        return OWN_WRITES.get()[0] > 0;
    }

    /**
     * Whether the block at {@code pos}, a root or a segment between the root and the top, has lost the segment
     * its plant stands on it: it is old enough to carry one, the block above is no longer a segment of this crop,
     * and the change was not one of the plant's own. Something other than a player took that segment (a farmer,
     * a harvester, a piston, water, an explosion that missed the root, a command), and a plant with its top gone
     * is a plant broken: the crop's shape update then turns this block to air, which the game breaks with its
     * drops, so the harvest falls once, from the root, as when a player breaks any block of the plant. A player's
     * break never gets here, because {@code playerWillDestroy} takes the rest of the plant first, as its own write.
     *
     * <p>The block above is read from the level, not taken from the update: an update raised inside another one
     * waits in the game's queue with the state it was raised with, and by the time it runs the plant may have
     * grown its top back. For a plant that hangs, "above" is the block below: the next one the way it grows.
     */
    static boolean lostItsTop(Block crop, LevelReader world, BlockPos pos, boolean carriesOne) {
        if (!carriesOne || inOwnWrite()) {
            return false;
        }
        BlockState next = world.getBlockState(pos.relative(growth(crop)));
        return !next.is(crop) || isRoot(next);
    }
}
