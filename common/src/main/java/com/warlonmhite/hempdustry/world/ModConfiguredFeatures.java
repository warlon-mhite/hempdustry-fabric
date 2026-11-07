package com.warlonmhite.hempdustry.world;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.world.gen.BeldiaPatchFeature;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.blockplacers.SimpleBlockPlacer;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import com.google.common.collect.ImmutableList;

/**
 * The wild plants' patches. Minecraft 1.16.5 registers worldgen in code: each patch is a configured
 * feature in vanilla's built-in registry, under the id the newer lines' data file has, and
 * {@link ModPlacedFeatures} decorates it.
 */
public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> INDICA_KEY = registerKey("indica");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_KEY = registerKey("sativa");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_SPARSE_KEY = registerKey("sativa_sparse");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_RARE_KEY = registerKey("sativa_rare");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BELDIA_KEY = registerKey("beldia");
    // No indica_cave: its tier grows in lush caves, which came with 1.17.

    /**
     * Wild Beldía's own patch. The newer lines build it from vanilla's random patch, a block column
     * and a filter; 1.16.5 has neither the column nor a filter per plant, so the patch is a feature of
     * its own that asks the same questions in the same order.
     */
    public static final Feature<NoneFeatureConfiguration> BELDIA_PATCH = Registry.register(Registry.FEATURE,
            new ResourceLocation(Hempdustry.MOD_ID, "beldia_patch"), new BeldiaPatchFeature(NoneFeatureConfiguration.CODEC));

    /**
     * A wild patch is usually small and one in ten is a big one — vanilla's own 64-try flower
     * patch. Most tries land nothing: a try only counts where the offset's {@code dy} is 0 and the
     * spot is still air, and the grass vanilla has already placed takes most of the air. Measured
     * with {@code /place feature} in real terrain on the newer lines, a try lands about 0.1 plants in
     * plains and less in savanna, so 24 tries gives 1-4 plants in most patches and 64 gives the lucky
     * 6-8. A single fixed {@code tries} cannot give that shape: anything high enough for the lucky tail
     * makes it the norm on bare ground. Vanilla uses two features for the same job (a common and a
     * rare melon or berry patch); a selector does it inside one, so no id changes.
     */
    private static final int SMALL_TRIES = 24;
    private static final int BIG_TRIES = 64;
    private static final float BIG_CHANCE = 0.1F;

    // Surface: a small patch on the terrain heightmap, now and then a big one.
    public static final ConfiguredFeature<?, ?> INDICA = register(INDICA_KEY, wildPatch(ModBlocks.INDICA_FLOWER));

    // Wild Lemon Haze. Sativa is a sun plant — no cave counterpart. Three surface tiers, thinning out
    // as the ground gets harsher; each is paired with a rarity in ModPlacedFeatures and a biome
    // selector, and the three selectors are kept strictly disjoint so nothing ever generates twice.
    //
    //   sativa        savanna & warm open grassland — the strain's home range, and the only
    //                 sativa tier with a lucky big patch
    //   sativa_sparse wooded badlands & arid scrub  — it hangs on (1-3 plants)
    //   sativa_rare   badlands & eroded badlands    — a straggly plant, often none
    public static final ConfiguredFeature<?, ?> SATIVA = register(SATIVA_KEY, wildPatch(ModBlocks.SATIVA_FLOWER));
    public static final ConfiguredFeature<?, ?> SATIVA_SPARSE = register(SATIVA_SPARSE_KEY, patch(ModBlocks.SATIVA_FLOWER, 6));
    public static final ConfiguredFeature<?, ?> SATIVA_RARE = register(SATIVA_RARE_KEY, patch(ModBlocks.SATIVA_FLOWER, 3));

    // Wild Beldía is the crop itself, ripe, standing where sugar cane would: on sand with water right
    // beside it, so a desert riverbank. See BeldiaPatchFeature.
    public static final ConfiguredFeature<?, ?> BELDIA = register(BELDIA_KEY,
            BELDIA_PATCH.configured(NoneFeatureConfiguration.INSTANCE));

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registry.CONFIGURED_FEATURE_REGISTRY, new ResourceLocation(Hempdustry.MOD_ID, name));
    }

    /** Forces the registrations above; {@link ModPlacedFeatures} reads them. */
    public static void registerConfiguredFeatures() {
        Hempdustry.LOGGER.info("Registering Configured Features for " + Hempdustry.MOD_ID);
    }

    private static ConfiguredFeature<?, ?> wildPatch(Block flower) {
        return Feature.RANDOM_SELECTOR.configured(new RandomFeatureConfiguration(
                ImmutableList.of(patch(flower, BIG_TRIES).weighted(BIG_CHANCE)),
                patch(flower, SMALL_TRIES)));
    }

    /**
     * A patch that attempts {@code tries} plants within 6 blocks across and 2 up or down of its centre,
     * each only where the spot is air and the plant can stand — the newer lines' random patch of a
     * simple block placed only when empty, in 1.16.5's terms. The centre is where the placement put it
     * ({@code noProjection}): the decorator has already found the terrain's top.
     *
     * <p>{@code RANDOM_PATCH}, not {@code FLOWER}: the two generate identically, but bone-mealed grass
     * grows the first {@code FLOWER} feature of its biome, and in all three badlands ours would be
     * the only one — a free wild plant, and so free seeds, from a stack of bone meal.
     */
    private static ConfiguredFeature<?, ?> patch(Block flower, int tries) {
        return Feature.RANDOM_PATCH.configured(new RandomPatchConfiguration.GrassConfigurationBuilder(
                new SimpleStateProvider(flower.defaultBlockState()), SimpleBlockPlacer.INSTANCE)
                .tries(tries).xspread(6).yspread(2).zspread(6).noProjection().build());
    }

    private static ConfiguredFeature<?, ?> register(ResourceKey<ConfiguredFeature<?, ?>> key, ConfiguredFeature<?, ?> feature) {
        return BuiltinRegistries.register(BuiltinRegistries.CONFIGURED_FEATURE, key.location(), feature);
    }
}
