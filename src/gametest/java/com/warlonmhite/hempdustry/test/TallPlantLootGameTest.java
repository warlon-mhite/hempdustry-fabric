package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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
public final class TallPlantLootGameTest implements FabricGameTest {
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
            Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER);
    private static final BlockState UPPER =
            Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void tallGrassSeedPoolFiresOncePerPlant(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos lowerPos = context.getAbsolutePos(new BlockPos(1, 2, 1));
        BlockPos upperPos = lowerPos.up();
        LootTable table = world.getServer().getReloadableRegistries()
                .getLootTable(Blocks.TALL_GRASS.getLootTableKey());

        // FORCE_STATE and nothing else: no neighbour updates, so a half left on its own below sits
        // there to be rolled instead of immediately popping off.
        world.setBlockState(lowerPos, LOWER, Block.FORCE_STATE);
        world.setBlockState(upperPos, UPPER, Block.FORCE_STATE);

        // The whole plant is standing — this is the roll for the half the player actually hit.
        int hitLower = seedDrops(table, world, lowerPos, LOWER);
        int hitUpper = seedDrops(table, world, upperPos, UPPER);
        context.assertTrue(hitLower > 0,
                "the hemp seed pool never fired on a broken lower half in " + SEEDS + " rolls");
        context.assertTrue(hitUpper > 0,
                "the hemp seed pool never fired on a broken upper half in " + SEEDS + " rolls");

        // …and this is the second roll, as the orphan pops off with its partner already gone.
        world.setBlockState(upperPos, Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
        int orphanLower = seedDrops(table, world, lowerPos, LOWER);

        world.setBlockState(upperPos, UPPER, Block.FORCE_STATE);
        world.setBlockState(lowerPos, Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
        int orphanUpper = seedDrops(table, world, upperPos, UPPER);

        context.assertTrue(orphanLower == 0, "the orphaned lower half paid out " + orphanLower
                + " hemp seeds in " + SEEDS + " rolls — the plant drops at 1-(1-p)², not p");
        context.assertTrue(orphanUpper == 0, "the orphaned upper half paid out " + orphanUpper
                + " hemp seeds in " + SEEDS + " rolls — the plant drops at 1-(1-p)², not p");
        context.complete();
    }

    /** How many of {@link #SEEDS} fixed rolls of {@code table} hand over a hemp seed. */
    private static int seedDrops(LootTable table, ServerWorld world, BlockPos pos, BlockState state) {
        // LootContextParameterSet on this line; it is LootContextParameterSet from 1.21.5 on.
        LootContextParameterSet params = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                .add(LootContextParameters.BLOCK_STATE, state)
                // Empty rather than a real tool: the pool carries vanilla's "not sheared" gate.
                .add(LootContextParameters.TOOL, ItemStack.EMPTY)
                .build(LootContextTypes.BLOCK);

        java.util.Random seeds = new java.util.Random(SEED_SOURCE);
        int hits = 0;
        for (int roll = 0; roll < SEEDS; roll++) {
            long seed = seeds.nextLong();
            for (ItemStack stack : table.generateLoot(params, seed)) {
                if (stack.isIn(ModTags.Items.HEMP_SEEDS)) {
                    hits++;
                }
            }
        }
        return hits;
    }
}
