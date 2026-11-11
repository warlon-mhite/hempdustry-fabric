package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A ripe plant whose top is taken by something other than a player comes down whole, and its harvest falls once,
 * from the root, as when a player breaks any block of it.
 *
 * <p>Every pool of both crops sits on the root, so a top broken alone drops nothing; before, the root stayed
 * behind, ripe and alone. A farmer reaping the block above its feet took the top first, and Create's harvester,
 * sweeping at the top's height, broke the top and then wrote it back at age 0 onto the ripe root. Each test takes
 * every block above the root of a ripe indica and a ripe sativa the way one of those hands does, and counts the
 * plant's seeds on the ground.
 *
 * <p>A natural ripe plant's seeds come from one pool only: 3, plus a binomial of 3 at 0.4 with no Fortune. So one
 * harvest drops 3 to 6 seeds, and none or two of them fall outside that; each case is broken {@link #REPEATS}
 * times, so that a harvest paid twice cannot hide behind four lowest rolls in a row.
 */
public final class TallPlantBreakGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    /** The tallest plant is three blocks; one more is checked to be empty. */
    private static final int COLUMN = 4;
    private static final int REPEATS = 4;
    private static final int FEWEST_SEEDS = 3;
    private static final int MOST_SEEDS = 6;

    private static final Map<Block, Item> SEEDS = Map.of(
            ModBlocks.INDICA_CROP, ModItems.INDICA_SEEDS,
            ModBlocks.SATIVA_CROP, ModItems.SATIVA_SEEDS);

    /** A farmer reaps the block above its feet with {@code destroyBlock}, the top of a plant as soon as the root. */
    public static void aTopAFarmerReapsBringsThePlantDown(GameTestHelper context) {
        everyTop(context, "reaped by a farmer", (world, at) -> {
            world.destroyBlock(at, true, null);
            return List.of();
        });
        context.succeed();
    }

    /**
     * Create's harvester, at the top's height: it breaks the top with the block's own drops (none) and its
     * after-break hook, sets it to air, then writes the cut block back at age 0 wherever that can survive. With
     * the root gone, it cannot, and nothing stands on the soil.
     */
    public static void aTopAHarvesterCutsBringsThePlantDown(GameTestHelper context) {
        everyTop(context, "cut by a harvester", (world, at) -> {
            BlockState cut = world.getBlockState(at);
            List<ItemStack> collected = Block.getDrops(cut, world, at, null, null, ItemStack.EMPTY);
            cut.spawnAfterBreak(world, at, ItemStack.EMPTY, false);
            world.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            BlockState replant = cut.setValue(BlockStateProperties.AGE_7, 0);
            world.setBlockAndUpdate(at, replant.canSurvive(world, at) ? replant : Blocks.AIR.defaultBlockState());
            return collected;
        });
        context.succeed();
    }

    /**
     * An explosion that reaches a block above the root takes the root as that explosion: the harvest goes into the
     * explosion's drops (where its decay applies), not whole onto the ground through the root's shape update.
     * Radius 1 decays nothing, so the count is the plain harvest's.
     */
    public static void aTopAnExplosionReachesTakesTheRootAsTheExplosion(GameTestHelper context) {
        everyTop(context, "hit by an explosion", (world, at) -> {
            Explosion explosion = new ServerExplosion(world, null, null, null, Vec3.atCenterOf(at),
                    1.0F, false, Explosion.BlockInteraction.DESTROY_WITH_DECAY);
            List<ItemStack> blown = new ArrayList<>();
            BlockState hit = world.getBlockState(at);
            hit.onExplosionHit(world, at, explosion, (stack, pos) -> blown.add(stack));
            int seeds = blown.stream().filter(stack -> stack.is(SEEDS.get(hit.getBlock())))
                    .mapToInt(ItemStack::getCount).sum();
            context.assertTrue(seeds >= FEWEST_SEEDS, BuiltInRegistries.BLOCK.getKey(hit.getBlock()).getPath()
                    + " hit by an explosion above its root: the explosion's drops hold " + seeds
                    + " seeds, so the root's harvest did not go through the explosion");
            return blown;
        });
        context.succeed();
    }

    /**
     * A player's break was already whole and paid once; it still is, now that a root left without its top comes
     * down by itself. The player's own path takes the rest of the plant first, as the plant's own write, so the
     * root's harvest is never paid both by hand and by its shape update. Every block of the plant, the root too.
     */
    public static void aPlayerBreakingAnyBlockHarvestsThePlantOnce(GameTestHelper context) {
        ServerPlayer player = Players.survival(context);
        for (Block crop : SEEDS.keySet()) {
            int height = wholePlant(crop).size();
            for (int up = 0; up < height; up++) {
                for (int repeat = 0; repeat < REPEATS; repeat++) {
                    ground(context);
                    place(context, crop);
                    Players.breakAs(context, player, CROP.above(up));
                    expectHarvestedOnce(context, crop, "broken by a player at block " + up, List.of());
                }
            }
        }
        context.succeed();
    }

    // ----- helpers -----

    /** A hand other than a player's taking the block at {@code at}; answers what it kept of the drops itself. */
    private interface Hand {
        List<ItemStack> take(ServerLevel world, BlockPos at);
    }

    private static void everyTop(GameTestHelper context, String how, Hand hand) {
        for (Block crop : SEEDS.keySet()) {
            int height = wholePlant(crop).size();
            for (int up = 1; up < height; up++) {
                for (int repeat = 0; repeat < REPEATS; repeat++) {
                    ground(context);
                    place(context, crop);
                    List<ItemStack> kept = hand.take(context.getLevel(), context.absolutePos(CROP.above(up)));
                    expectHarvestedOnce(context, crop, how + " at block " + up, kept);
                }
            }
        }
    }

    private static String name(Block crop) {
        return BuiltInRegistries.BLOCK.getKey(crop).getPath();
    }

    /**
     * Empties the column top-down and the ground of items, then lays wet farmland. Glowstone beside the root keeps
     * it lit whatever the hour, since a root updated by a neighbour breaks in the dark.
     */
    private static void ground(GameTestHelper context) {
        for (int up = COLUMN - 1; up >= 0; up--) {
            context.setBlock(CROP.above(up), Blocks.AIR);
        }
        context.getEntities(EntityType.ITEM).forEach(ItemEntity::discard);
        context.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        context.setBlock(CROP.east(), Blocks.GLOWSTONE);
    }

    /** Every block of a ripe, untrimmed, naturally lit plant, bottom up, as the crop's own growth writes them. */
    private static List<BlockState> wholePlant(Block crop) {
        BlockState ripe = Defoliation.unworked(crop.defaultBlockState())
                .setValue(BlockStateProperties.AGE_7, IndicaCropBlock.MAX_AGE);
        List<BlockState> blocks = new ArrayList<>(List.of(ripe));
        if (crop instanceof SativaCropBlock) {
            blocks.add(ripe.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.MIDDLE));
            blocks.add(ripe.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.UPPER));
        } else {
            blocks.add(ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER));
        }
        return blocks;
    }

    private static void place(GameTestHelper context, Block crop) {
        List<BlockState> blocks = wholePlant(crop);
        for (int up = 0; up < blocks.size(); up++) {
            context.setBlock(CROP.above(up), blocks.get(up));
        }
    }

    private static void expectHarvestedOnce(GameTestHelper context, Block crop, String how, List<ItemStack> kept) {
        for (int up = 0; up < COLUMN; up++) {
            BlockState got = context.getBlockState(CROP.above(up));
            context.assertTrue(got.isAir(), name(crop) + " " + how + ": block " + up + " above the soil is " + got
                    + ", expected the whole plant gone");
        }
        Item seed = SEEDS.get(crop);
        int seeds = kept.stream().filter(stack -> stack.is(seed)).mapToInt(ItemStack::getCount).sum();
        for (ItemEntity item : context.getEntities(EntityType.ITEM)) {
            if (item.getItem().is(seed)) {
                seeds += item.getItem().getCount();
            }
        }
        context.assertTrue(seeds >= FEWEST_SEEDS && seeds <= MOST_SEEDS, name(crop) + " " + how + " dropped " + seeds
                + " seeds, where one harvest drops " + FEWEST_SEEDS + " to " + MOST_SEEDS);
    }
}
