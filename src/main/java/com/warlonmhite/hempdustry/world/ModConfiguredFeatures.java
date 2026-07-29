package com.warlonmhite.hempdustry.world;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class ModConfiguredFeatures {

    public static final RegistryKey<ConfiguredFeature<?, ?>> INDICA_KEY = registerKey("indica");
    public static final RegistryKey<ConfiguredFeature<?, ?>> INDICA_CAVE_KEY = registerKey("indica_cave");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SATIVA_KEY = registerKey("sativa");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SATIVA_SPARSE_KEY = registerKey("sativa_sparse");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SATIVA_RARE_KEY = registerKey("sativa_rare");

    public static RegistryKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(Hempdustry.MOD_ID, name));
    }

    public static void bootstrap(Registerable<ConfiguredFeature<?, ?>> context) {
        // Surface: a small flower patch placed on the terrain heightmap. `tries` is the flower
        // density within a patch (attempts to place a flower); lowered from 32 to thin out the batch.
        register(context, INDICA_KEY, Feature.FLOWER, new RandomPatchFeatureConfig(16, 6, 2, PlacedFeatures.createEntry(Feature.SIMPLE_BLOCK,
                new SimpleBlockFeatureConfig(BlockStateProvider.of(ModBlocks.INDICA_FLOWER)))));

        // Caves: a single flower; the cave placed feature scatters these across cave
        // floors, and SimpleBlockFeature's own canPlaceAt check keeps them on valid
        // ground (moss/dirt) rather than bare stone.
        register(context, INDICA_CAVE_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockFeatureConfig(BlockStateProvider.of(ModBlocks.INDICA_FLOWER)));

        // Wild Lemon Haze. Sativa is a sun plant — no cave counterpart, unlike indica. Three
        // surface tiers of the same patch, thinning out as the ground gets harsher; each is paired
        // with a rarity in ModPlacedFeatures and a biome tag, and the three biome tags are kept
        // strictly disjoint so nothing ever generates twice.
        //
        //   sativa        savanna & warm open grassland — the strain's home range
        //   sativa_sparse wooded badlands & arid scrub  — it hangs on
        //   sativa_rare   badlands & eroded badlands    — a couple of straggly plants
        //
        // `tries` is how many flowers a single patch attempts, so it is the within-patch density.
        register(context, SATIVA_KEY, sativaPatch(12));
        register(context, SATIVA_SPARSE_KEY, sativaPatch(6));
        register(context, SATIVA_RARE_KEY, sativaPatch(3));
    }

    /** A wild Lemon Haze flower patch that attempts {@code tries} flowers. */
    private static ConfiguredFeature<?, ?> sativaPatch(int tries) {
        return new ConfiguredFeature<>(Feature.FLOWER, new RandomPatchFeatureConfig(tries, 6, 2,
                PlacedFeatures.createEntry(Feature.SIMPLE_BLOCK,
                        new SimpleBlockFeatureConfig(BlockStateProvider.of(ModBlocks.SATIVA_FLOWER)))));
    }

    private static void register(Registerable<ConfiguredFeature<?, ?>> context,
                                 RegistryKey<ConfiguredFeature<?, ?>> key, ConfiguredFeature<?, ?> feature) {
        context.register(key, feature);
    }

    private static <FC extends FeatureConfig, F extends Feature<FC>> void register(Registerable<ConfiguredFeature<?, ?>> context,
                                                                                   RegistryKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
