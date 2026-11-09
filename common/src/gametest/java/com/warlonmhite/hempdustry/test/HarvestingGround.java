package com.warlonmhite.hempdustry.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Farmland that harvests the crop on it the moment it ripens, as some other mods' soils do (Just Dire Things'
 * goo soil, from its second tier on). Such a soil answers the shape update its crop's write sends down to it:
 * it takes the root's drops, breaks the root and plants the crop's default state in its place, all inside the
 * write that ripened the plant. Only the farmland blocks a test {@link #watch watches} do this
 * ({@code FarmBlockHarvestMixin}); every other farmland in the game-test world is vanilla's.
 */
public final class HarvestingGround {
    private static final Set<BlockPos> watched = new HashSet<>();
    private static final List<ItemStack> taken = new ArrayList<>();

    private HarvestingGround() {
    }

    /** Makes the farmland at {@code soil} (absolute) harvest from now on, forgetting what was taken before. */
    public static void watch(BlockPos soil) {
        watched.add(soil.immutable());
        taken.clear();
    }

    public static void forget(BlockPos soil) {
        watched.remove(soil);
    }

    /** What the watched grounds harvested since the last {@link #watch}. */
    public static List<ItemStack> taken() {
        return List.copyOf(taken);
    }

    /** From the mixin: farmland at {@code pos} told that the block on {@code direction} is now {@code neighbor}. */
    public static void answer(LevelAccessor level, BlockPos pos, Direction direction, BlockState neighbor,
                              BlockPos neighborPos) {
        if (direction != Direction.UP || !watched.contains(pos) || !(level instanceof ServerLevel server)
                || !(neighbor.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(neighbor)) {
            return;
        }
        taken.addAll(Block.getDrops(neighbor, server, neighborPos, null));
        server.destroyBlock(neighborPos, false);
        server.setBlockAndUpdate(neighborPos, crop.defaultBlockState());
    }
}
