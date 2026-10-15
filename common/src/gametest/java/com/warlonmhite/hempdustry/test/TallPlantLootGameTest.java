package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * The hemp-seed pool injected into tall grass must pay out once per plant, not twice.
 *
 * <p>Breaking one half of a two-block plant rolls its loot table <b>twice</b>: once for the half the
 * player hit, and once more as the orphaned half pops off. A pool injected by
 * {@code LootTableEvents.MODIFY} inherits neither of vanilla's guards, so without the
 * "this half was broken AND the other half is still standing" pair it fires on both rolls and drops
 * at {@code 1-(1-p)²} instead of {@code p}. Silent, and in the generous direction.
 *
 * <p>Rolled over a fixed span of seeds rather than sampled from the world's random, so the counts
 * below are the same on every run — the test either passes forever or fails forever, never
 * sometimes. 1% over 2000 seeds puts the expected hit count around 20.
 */
public final class TallPlantLootGameTest {
    /** Enough fixed seeds that a 1% pool is certain to fire, and cheap: no world mutation per roll. */
    private static final int SEEDS = 2000;

    /**
     * Fixed, so the counts below are identical on every run — but <b>not</b> {@code 0, 1, 2, …}.
     * A loot seed is scrambled once into a {@code Random}, and consecutive seeds leave the first
     * draw of that stream strongly correlated: over seeds 0-1999 the pool's chance roll never once
     * came out under 1%, so the orphan count sat at a flat zero whether the guard was there or not
     * and the test passed with the guard deleted. Drawing the seeds themselves from a seeded
     * {@code Random} spreads them, and the mutation goes red as it should.
     */
    private static final long SEED_SOURCE = 0x48454D50L; // "HEMP"

    private static final BlockState LOWER =
            Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
    private static final BlockState UPPER =
            Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void tallGrassSeedPoolFiresOncePerPlant(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos lowerPos = context.absolutePos(new BlockPos(1, 2, 1));
        BlockPos upperPos = lowerPos.above();
        LootTable table = world.getServer().reloadableRegistries()
                .getLootTable(Blocks.TALL_GRASS.getLootTable());

        // FORCE_STATE and nothing else: no neighbour updates, so a half left on its own below sits
        // there to be rolled instead of immediately popping off.
        world.setBlock(lowerPos, LOWER, Block.UPDATE_KNOWN_SHAPE);
        world.setBlock(upperPos, UPPER, Block.UPDATE_KNOWN_SHAPE);

        // The whole plant is standing — this is the roll for the half the player actually hit.
        int hitLower = seedDrops(table, world, lowerPos, LOWER);
        int hitUpper = seedDrops(table, world, upperPos, UPPER);
        context.assertTrue(hitLower > 0,
                "the hemp seed pool never fired on a broken lower half in " + SEEDS + " rolls");
        context.assertTrue(hitUpper > 0,
                "the hemp seed pool never fired on a broken upper half in " + SEEDS + " rolls");

        // …and this is the second roll, as the orphan pops off with its partner already gone.
        world.setBlock(upperPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE);
        int orphanLower = seedDrops(table, world, lowerPos, LOWER);

        world.setBlock(upperPos, UPPER, Block.UPDATE_KNOWN_SHAPE);
        world.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE);
        int orphanUpper = seedDrops(table, world, upperPos, UPPER);

        context.assertTrue(orphanLower == 0, "the orphaned lower half paid out " + orphanLower
                + " hemp seeds in " + SEEDS + " rolls — the plant drops at 1-(1-p)², not p");
        context.assertTrue(orphanUpper == 0, "the orphaned upper half paid out " + orphanUpper
                + " hemp seeds in " + SEEDS + " rolls — the plant drops at 1-(1-p)², not p");
        context.succeed();
    }

    /** How many of {@link #SEEDS} fixed rolls of {@code table} hand over a hemp seed. */
    private static int seedDrops(LootTable table, ServerLevel world, BlockPos pos, BlockState state) {
        // LootParams, by that name on this line and from 1.21.5 on.
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                // Empty rather than a real tool: the pool carries vanilla's "not sheared" gate.
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);

        java.util.Random seeds = new java.util.Random(SEED_SOURCE);
        int hits = 0;
        for (int roll = 0; roll < SEEDS; roll++) {
            long seed = seeds.nextLong();
            for (ItemStack stack : table.getRandomItems(params, seed)) {
                if (stack.is(ModTags.Items.HEMP_SEEDS)) {
                    hits++;
                }
            }
        }
        return hits;
    }
}
