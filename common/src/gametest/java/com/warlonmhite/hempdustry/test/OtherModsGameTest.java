package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TallHemp;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * What other mods read off our plants, held to what they expect of any crop.
 *
 * <p>Harvesters in other mods (Industrial Foregoing's plant gatherer, Create's mechanical harvester) take any
 * {@code CropBlock} whose {@code isMaxAge} holds, one block at a time; when the blocks above a tall plant's
 * root claimed to be ripe, they cut a growing plant down to its root for no drops. And mods that react to
 * growth hear it through the loader's crop-growth events, which a crop with its own growth step has to fire
 * itself.
 */
public final class OtherModsGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();

    /** A random that lands every roll, so one random tick certainly grows the plant. */
    private static final RandomSource HIT = new SingleThreadedRandomSource(0L) {
        @Override
        public float nextFloat() {
            return 0.0F;
        }
    };

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyBlockOfAPlantTellsItsTrueAge(GameTestHelper context) {
        Map<String, String> findings = new TreeMap<>();
        int seen = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof CropBlock crop) || !Sweep.ours(BuiltInRegistries.BLOCK.getKey(block))) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (!state.hasProperty(BlockStateProperties.AGE_7)) {
                    findings.put(state.toString(), "a crop of ours without the age property this sweep reads");
                    continue;
                }
                boolean ripe = state.getValue(BlockStateProperties.AGE_7) >= crop.getMaxAge();
                if (crop.isMaxAge(state) != ripe) {
                    findings.put(state.toString(), "isMaxAge says " + crop.isMaxAge(state) + " at age "
                            + state.getValue(BlockStateProperties.AGE_7));
                }
                seen++;
            }
        }
        Sweep.atLeast(context, "crop block states", seen, 100);
        Sweep.report(context, "every block of a crop of ours answers isMaxAge with its own age", findings, Map.of());
        context.succeed();
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void onlyTheRootIsTheRoot(GameTestHelper context) {
        for (Block block : List.of(ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP, ModBlocks.BELDIA_CROP)) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                boolean lower = state.hasProperty(SativaCropBlock.SEGMENT)
                        ? state.getValue(SativaCropBlock.SEGMENT) == TriplePlantSegment.LOWER
                        : state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
                context.assertTrue(TallHemp.isRoot(state) == lower, "TallHemp.isRoot is " + TallHemp.isRoot(state)
                        + " for " + state);
            }
        }
        context.assertTrue(TallHemp.isRoot(Blocks.WHEAT.defaultBlockState()), "wheat is not its own root");
        context.succeed();
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void growingAsksTheLoaderAndTellsIt(GameTestHelper context) {
        TestPlatform.CropGrowthProbe probe = TestPlatform.INSTANCE.cropGrowthProbe();
        if (probe == null) {
            context.succeed(); // no crop-growth events on this loader
            return;
        }
        context.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        context.setBlock(CROP.east(), Blocks.GLOWSTONE);
        context.setBlock(CROP, Defoliation.unworked(ModBlocks.INDICA_CROP.defaultBlockState()));
        // Lit next tick, once the light engine has run.
        context.runAfterDelay(5, () -> {
            ServerLevel world = context.getLevel();
            BlockPos pos = context.absolutePos(CROP);
            try {
                for (Block block : List.of(ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP)) {
                    String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
                    world.setBlock(pos, Defoliation.unworked(block.defaultBlockState()), Block.UPDATE_ALL);

                    probe.watch(pos, true);
                    world.getBlockState(pos).randomTick(world, pos, HIT);
                    int held = world.getBlockState(pos).getValue(BlockStateProperties.AGE_7);
                    List<String> heldHeard = probe.take();
                    context.assertTrue(held == 0, name + " grew to " + held + " although the loader's listener said not to");
                    context.assertTrue(heldHeard.equals(List.of("PRE")), name + " held back, heard " + heldHeard
                            + ", expected [PRE]");

                    probe.watch(pos, false);
                    world.getBlockState(pos).randomTick(world, pos, HIT);
                    int grown = world.getBlockState(pos).getValue(BlockStateProperties.AGE_7);
                    List<String> grownHeard = probe.take();
                    context.assertTrue(grown == 1, name + " did not grow on a landed roll: age " + grown);
                    context.assertTrue(grownHeard.equals(List.of("PRE", "POST")), name + " grew, heard " + grownHeard
                            + ", expected [PRE, POST]");
                }
            } finally {
                probe.stop();
            }
            context.succeed();
        });
    }

    /**
     * A plant held back by its headroom still rolls, and the loader is asked each time, as vanilla's crops ask
     * it; but it is told the plant grew only when the age really moved. A listener paid per growth (Ars
     * Nouveau's Agronomic Sourcelink) was paid for every roll of a roofed seedling that never grew.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aPlantWithNoRoomToGrowTellsNobodyItGrew(GameTestHelper context) {
        TestPlatform.CropGrowthProbe probe = TestPlatform.INSTANCE.cropGrowthProbe();
        if (probe == null) {
            context.succeed(); // no crop-growth events on this loader
            return;
        }
        context.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        context.setBlock(CROP.east(), Blocks.GLOWSTONE);
        // The last age that fits in one block: the next one needs the block above.
        Map<Block, Integer> lastLone = Map.of(
                ModBlocks.INDICA_CROP, IndicaCropBlock.DOUBLE_BLOCK_AGE - 1,
                ModBlocks.SATIVA_CROP, SativaCropBlock.TWO_TALL_AGE - 1);
        context.runAfterDelay(5, () -> {
            ServerLevel world = context.getLevel();
            BlockPos pos = context.absolutePos(CROP);
            try {
                for (Block block : List.of(ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP)) {
                    String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
                    int age = lastLone.get(block);
                    world.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    world.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    world.setBlock(pos, Defoliation.unworked(block.defaultBlockState())
                            .setValue(BlockStateProperties.AGE_7, age), Block.UPDATE_ALL);

                    probe.watch(pos, false);
                    world.getBlockState(pos).randomTick(world, pos, HIT);
                    int held = world.getBlockState(pos).getValue(BlockStateProperties.AGE_7);
                    List<String> heldHeard = probe.take();
                    context.assertTrue(held == age, name + " grew to " + held + " under a roof");
                    context.assertTrue(heldHeard.equals(List.of("PRE")), name + " roofed at age " + age + ", heard "
                            + heldHeard + ", expected [PRE]");

                    world.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    probe.watch(pos, false);
                    world.getBlockState(pos).randomTick(world, pos, HIT);
                    int grown = world.getBlockState(pos).getValue(BlockStateProperties.AGE_7);
                    List<String> grownHeard = probe.take();
                    context.assertTrue(grown == age + 1, name + " did not grow once the roof was gone: age " + grown);
                    context.assertTrue(grownHeard.equals(List.of("PRE", "POST")), name + " grew, heard " + grownHeard
                            + ", expected [PRE, POST]");
                }
            } finally {
                probe.stop();
            }
            context.succeed();
        });
    }
    /**
     * Our seeds and fibre carry the common tags other mods' machines and pack scripts read on 1.20.1, in both
     * namespaces a pack there may use: every hemp seed in {@code forge:seeds} and {@code c:seeds} (the seed-oil
     * presses compact any seed in {@code #forge:seeds}), hemp fibre in {@code forge:fiber_hemp} and
     * {@code c:fiber_hemp} (Immersive Engineering's name for it). And none of ours in Immersive Engineering's
     * {@code seeds/hemp} (its own industrial hemp seed, which a pack or a unifier asking for it means) or
     * {@code fabric_hemp} (its fabric takes eight fibre and a stick; our canvas takes four and turns back into
     * them, so in that tag it would halve the cost of everything made from the fabric).
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void ourSeedsAndFibreCarryTheCommonTags(GameTestHelper context) {
        Map<String, String> findings = new TreeMap<>();
        List<Item> seeds = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(ModTags.Items.HEMP_SEEDS).forEach(holder -> seeds.add(holder.value()));
        Sweep.atLeast(context, "hemp seeds of ours", seeds.size(), 3);
        for (String ns : List.of("forge", "c")) {
            for (Item seed : seeds) {
                expectIn(findings, seed, tag(ns, "seeds"), true);
                expectIn(findings, seed, tag(ns, "seeds/hemp"), false);
            }
            expectIn(findings, ModItems.HEMP_FIBER, tag(ns, "fiber_hemp"), true);
            expectIn(findings, ModItems.HEMP_CANVAS, tag(ns, "fabric_hemp"), false);
        }
        Sweep.report(context, "our seeds and fibre carry the common tags, and only those", findings, Map.of());
        context.succeed();
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(namespace, path));
    }

    private static void expectIn(Map<String, String> findings, Item item, TagKey<Item> tag, boolean wanted) {
        if (item.builtInRegistryHolder().is(tag) != wanted) {
            findings.put(BuiltInRegistries.ITEM.getKey(item) + " in #" + tag.location(),
                    wanted ? "missing from the tag" : "in the tag, and should not be");
        }
    }

}
