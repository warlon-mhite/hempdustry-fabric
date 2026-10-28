package com.warlonmhite.hempdustry.world;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import java.util.List;

/**
 * What a wild plant is: one block, or Beldía's two-block column.
 *
 * <p>Up to 1.21.11 each surface feature here was a whole patch — vanilla's {@code random_patch},
 * so many tries scattered around a centre — and the indica and home-range sativa ones a selector
 * between a small and a big patch. 26.1 removed the patch feature: a patch is now placement, a count
 * and a triangular offset after the placed feature's own placement, which is how vanilla writes its
 * flowers and sugar cane too. So this file says what grows, {@link ModPlacedFeatures} says how many
 * and where, and every id is the one 2.1.0 shipped.
 */
public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> INDICA_KEY = registerKey("indica");
    public static final ResourceKey<ConfiguredFeature<?, ?>> INDICA_CAVE_KEY = registerKey("indica_cave");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_KEY = registerKey("sativa");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_SPARSE_KEY = registerKey("sativa_sparse");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_RARE_KEY = registerKey("sativa_rare");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BELDIA_KEY = registerKey("beldia");

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name));
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Surface: a wild plant, scattered into a patch by its placed feature.
        register(context, INDICA_KEY, flower(ModBlocks.INDICA_FLOWER));

        // Caves: a single flower; the cave placed feature scatters these across cave
        // floors, and SimpleBlockFeature's own canSurvive check keeps them on valid
        // ground (moss/dirt) rather than bare stone.
        register(context, INDICA_CAVE_KEY, flower(ModBlocks.INDICA_FLOWER));

        // Wild Lemon Haze. Sativa is a sun plant — no cave counterpart, unlike indica. Three
        // surface tiers, thinning out as the ground gets harsher; each is paired with a patch size
        // and a rarity in ModPlacedFeatures and a biome tag, and the three biome tags are kept
        // strictly disjoint so nothing ever generates twice.
        //
        //   sativa        savanna & warm open grassland — the strain's home range, and the only
        //                 sativa tier with a lucky big patch
        //   sativa_sparse wooded badlands & arid scrub  — it hangs on (1-3 plants)
        //   sativa_rare   badlands & eroded badlands    — a straggly plant, often none
        register(context, SATIVA_KEY, flower(ModBlocks.SATIVA_FLOWER));
        register(context, SATIVA_SPARSE_KEY, flower(ModBlocks.SATIVA_FLOWER));
        register(context, SATIVA_RARE_KEY, flower(ModBlocks.SATIVA_FLOWER));

        // Wild Beldía is the crop itself, ripe, standing where sugar cane would: on sand with water
        // right beside it, so a desert riverbank. BLOCK_COLUMN puts the LOWER and the UPPER down
        // together, and the placed feature's filter wants air for both, so a plant is never cut
        // short to a lone half. Where it may stand is the placed feature's to say.
        BlockState ripe = ModBlocks.BELDIA_CROP.defaultBlockState().setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
        register(context, BELDIA_KEY, Feature.BLOCK_COLUMN, new BlockColumnConfiguration(List.of(
                BlockColumnConfiguration.layer(ConstantInt.of(1),
                        BlockStateProvider.simple(ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER))),
                BlockColumnConfiguration.layer(ConstantInt.of(1),
                        BlockStateProvider.simple(ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER)))),
                Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, false));
    }

    /**
     * One wild flower. A plain block, never in {@code #minecraft:can_spawn_from_bone_meal}: bone meal
     * on grass grows only the features in that tag, so a wild plant — and so free seeds — never comes
     * out of a stack of bone meal.
     */
    private static ConfiguredFeature<?, ?> flower(Block flower) {
        return new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(flower)));
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
