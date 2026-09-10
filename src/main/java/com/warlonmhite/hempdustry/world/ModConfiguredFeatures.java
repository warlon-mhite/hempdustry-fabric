package com.warlonmhite.hempdustry.world;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.List;

public class ModConfiguredFeatures {

    public static final RegistryKey<ConfiguredFeature<?, ?>> INDICA_KEY = registerKey("indica");
    public static final RegistryKey<ConfiguredFeature<?, ?>> INDICA_CAVE_KEY = registerKey("indica_cave");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SATIVA_KEY = registerKey("sativa");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SATIVA_SPARSE_KEY = registerKey("sativa_sparse");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SATIVA_RARE_KEY = registerKey("sativa_rare");

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

    public static RegistryKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(Hempdustry.MOD_ID, name));
    }

    public static void bootstrap(Registerable<ConfiguredFeature<?, ?>> context) {
        // Surface: a small patch on the terrain heightmap, now and then a big one.
        register(context, INDICA_KEY, wildPatch(ModBlocks.INDICA_FLOWER));

        // Caves: a single flower; the cave placed feature scatters these across cave
        // floors, and SimpleBlockFeature's own canPlaceAt check keeps them on valid
        // ground (moss/dirt) rather than bare stone.
        register(context, INDICA_CAVE_KEY, Feature.SIMPLE_BLOCK,
                new SimpleBlockFeatureConfig(BlockStateProvider.of(ModBlocks.INDICA_FLOWER)));

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
    }

    private static ConfiguredFeature<?, ?> wildPatch(Block flower) {
        return new ConfiguredFeature<>(Feature.RANDOM_SELECTOR, new RandomFeatureConfig(
                List.of(new RandomFeatureEntry(inline(patch(flower, BIG_TRIES)), BIG_CHANCE)),
                inline(patch(flower, SMALL_TRIES))));
    }

    /**
     * A patch that attempts {@code tries} plants. {@code RANDOM_PATCH}, not {@code FLOWER}: the two
     * generate identically, but bone-mealed grass grows the first {@code FLOWER} feature of its
     * biome, and in mangrove swamp and all three badlands ours would be the only one — a free wild
     * plant, and so free seeds, from a stack of bone meal.
     */
    private static ConfiguredFeature<?, ?> patch(Block flower, int tries) {
        return new ConfiguredFeature<>(Feature.RANDOM_PATCH, new RandomPatchFeatureConfig(tries, 6, 2,
                PlacedFeatures.createEntry(Feature.SIMPLE_BLOCK,
                        new SimpleBlockFeatureConfig(BlockStateProvider.of(flower)))));
    }

    /**
     * A selector entry with no placement of its own. Not {@code createEntry(Feature, config)}: that
     * adds an is-air filter, which here would test the patch's centre and drop the whole patch
     * whenever a tuft of grass stands on it.
     */
    private static RegistryEntry<PlacedFeature> inline(ConfiguredFeature<?, ?> feature) {
        return PlacedFeatures.createEntry(RegistryEntry.of(feature));
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
