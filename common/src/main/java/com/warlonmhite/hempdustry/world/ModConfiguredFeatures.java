package com.warlonmhite.hempdustry.world;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluids;
import java.util.List;

public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> INDICA_KEY = registerKey("indica");
    public static final ResourceKey<ConfiguredFeature<?, ?>> INDICA_CAVE_KEY = registerKey("indica_cave");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_KEY = registerKey("sativa");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_SPARSE_KEY = registerKey("sativa_sparse");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_RARE_KEY = registerKey("sativa_rare");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BELDIA_KEY = registerKey("beldia");

    /** Vanilla sugar cane's patch shape: 20 tries within 4 blocks, all on the centre's level. */
    private static final int BELDIA_TRIES = 20;
    public static final ResourceKey<ConfiguredFeature<?, ?>> DITCHWEED_KEY = registerKey("ditchweed");

    /** Vanilla sugar cane's patch shape: 20 tries within 4 blocks, all on the centre's level. */
    private static final int DITCHWEED_TRIES = 20;

    /**
     * A wild patch is usually small and one in ten is a big one — vanilla's own 64-try flower
     * patch. Most tries land nothing: a try only counts where the offset's {@code dy} is 0 and the
     * spot is still air, and the grass vanilla has already placed takes most of the air. Measured
     * with {@code /place feature} in real terrain, a try lands about 0.1 plants in plains and less
     * in savanna, so 24 tries gives 1-4 plants in most patches and 64 gives the lucky 6-8.
     * A single fixed {@code tries} cannot give that shape: anything high enough for the lucky tail
     * makes it the norm on bare ground. Vanilla uses two features for the same job (a common and a
     * rare melon or berry patch); a selector does it inside one, so no id changes.
     */
    private static final int SMALL_TRIES = 24;
    private static final int BIG_TRIES = 64;
    private static final float BIG_CHANCE = 0.1F;

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name));
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Surface: a small patch on the terrain heightmap, now and then a big one.
        register(context, INDICA_KEY, wildPatch(ModBlocks.INDICA_FLOWER));

        // Caves: a single flower; the cave placed feature scatters these across cave
        // floors, and SimpleBlockFeature's own canSurvive check keeps them on valid
        // ground (moss/dirt) rather than bare stone.
        register(context, INDICA_CAVE_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.INDICA_FLOWER)));

        // Wild Lemon Haze. Sativa is a sun plant — no cave counterpart, unlike indica. Three
        // surface tiers, thinning out as the ground gets harsher; each is paired with a rarity in
        // ModPlacedFeatures and a biome tag, and the three biome tags are kept strictly disjoint
        // so nothing ever generates twice.
        //
        //   sativa        savanna & warm open grassland — the strain's home range, and the only
        //                 sativa tier with a lucky big patch
        //   sativa_sparse wooded badlands & arid scrub  — it hangs on (1-3 plants)
        //   sativa_rare   badlands & eroded badlands    — a straggly plant, often none
        register(context, SATIVA_KEY, wildPatch(ModBlocks.SATIVA_FLOWER));
        register(context, SATIVA_SPARSE_KEY, patch(ModBlocks.SATIVA_FLOWER, 6));
        register(context, SATIVA_RARE_KEY, patch(ModBlocks.SATIVA_FLOWER, 3));

        // Wild Beldía is the crop itself, ripe, standing where sugar cane would: on sand with water
        // right beside it, so a desert riverbank. BLOCK_COLUMN puts the LOWER and the UPPER down
        // together, and the filter wants air for both, so a plant is never cut short to a lone half.
        //
        // The ground test is #hempdustry:beldia_soil -- the plant's own survival rule, so a modded
        // desert's sands work -- and it is asked directly rather than through wouldSurvive, because a
        // crop's canSurvive also reads light and worldgen has none yet. The water is vanilla's own four
        // offsets from nearWaterPredicate. InBeldiaBiome then checks each plant's own biome,
        // since the patch spreads past its centre's -- see that class. A desert shore grows it too.
        BlockState ripe = ModBlocks.BELDIA_CROP.defaultBlockState().setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
        register(context, BELDIA_KEY, Feature.RANDOM_PATCH, new RandomPatchConfiguration(BELDIA_TRIES, 4, 0,
                PlacementUtils.inlinePlaced(Feature.BLOCK_COLUMN,
                        new BlockColumnConfiguration(List.of(
                                BlockColumnConfiguration.layer(ConstantInt.of(1),
                                        BlockStateProvider.simple(ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER))),
                                BlockColumnConfiguration.layer(ConstantInt.of(1),
                                        BlockStateProvider.simple(ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER)))),
                                Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, false),
                        BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                BlockPredicate.matchesBlocks(new Vec3i(0, 1, 0), Blocks.AIR),
                                BlockPredicate.matchesTag(new Vec3i(0, -1, 0), ModTags.Blocks.BELDIA_SOIL),
                                BlockPredicate.anyOf(
                                        BlockPredicate.matchesFluids(new Vec3i(1, -1, 0), Fluids.WATER, Fluids.FLOWING_WATER),
                                        BlockPredicate.matchesFluids(new Vec3i(-1, -1, 0), Fluids.WATER, Fluids.FLOWING_WATER),
                                        BlockPredicate.matchesFluids(new Vec3i(0, -1, 1), Fluids.WATER, Fluids.FLOWING_WATER),
                                        BlockPredicate.matchesFluids(new Vec3i(0, -1, -1), Fluids.WATER, Fluids.FLOWING_WATER)))),
                        ModPlacementModifiers.InBeldiaBiomePlacementModifier.INSTANCE)));
        // Ditchweed — feral Industrial Hemp — grows where escaped fibre crops do, along water. So it
        // is placed exactly as sugar cane is: each plant must stand on its ground with water beside
        // that ground, vanilla's own filter, which is why most tries of a patch land nothing.
        register(context, DITCHWEED_KEY, Feature.RANDOM_PATCH, new RandomPatchConfiguration(DITCHWEED_TRIES, 4, 0,
                PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.DITCHWEED)),
                        VegetationFeatures.nearWaterPredicate(ModBlocks.DITCHWEED))));
    }

    private static ConfiguredFeature<?, ?> wildPatch(Block flower) {
        return new ConfiguredFeature<>(Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(inline(patch(flower, BIG_TRIES)), BIG_CHANCE)),
                inline(patch(flower, SMALL_TRIES))));
    }

    /**
     * A patch that attempts {@code tries} plants. {@code RANDOM_PATCH}, not {@code FLOWER}: the two
     * generate identically, but bone-mealed grass grows the first {@code FLOWER} feature of its
     * biome, and in mangrove swamp and all three badlands ours would be the only one — a free wild
     * plant, and so free seeds, from a stack of bone meal.
     */
    private static ConfiguredFeature<?, ?> patch(Block flower, int tries) {
        return new ConfiguredFeature<>(Feature.RANDOM_PATCH, new RandomPatchConfiguration(tries, 6, 2,
                PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(flower)))));
    }

    /**
     * A selector entry with no placement of its own. Not {@code createEntry(Feature, config)}: that
     * adds an is-air filter, which here would test the patch's centre and drop the whole patch
     * whenever a tuft of grass stands on it.
     */
    private static Holder<PlacedFeature> inline(ConfiguredFeature<?, ?> feature) {
        return PlacementUtils.inlinePlaced(Holder.direct(feature));
    }

    private static void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                 ResourceKey<ConfiguredFeature<?, ?>> key, ConfiguredFeature<?, ?> feature) {
        context.register(key, feature);
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                   ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
