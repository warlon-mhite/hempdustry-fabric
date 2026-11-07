package com.warlonmhite.hempdustry.world;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.Features;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Where each wild patch goes. Minecraft 1.16.5 has no placed features: a placement is a configured
 * feature decorated with vanilla's decorators, registered as a configured feature of its own. It keeps
 * the newer lines' placed-feature id ({@code hempdustry:indica_placed}), in the configured-feature
 * registry, and the same steps in the same order: a rarity, a spread across the chunk, the terrain's top.
 *
 * <p>The newer lines end every placement with a biome filter on the patch's centre. 1.16.5 has none:
 * a chunk is decorated with the features of the one biome found near its middle, so a patch whose
 * centre lands across a border, in that chunk, still grows — as every vanilla 1.16.5 flower does. The
 * odds per chunk are the same, so a biome holds the same number of patches.
 */
public class ModPlacedFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> INDICA_PLACED_KEY = registerKey("indica_placed");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_PLACED_KEY = registerKey("sativa_placed");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_SPARSE_PLACED_KEY = registerKey("sativa_sparse_placed");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATIVA_RARE_PLACED_KEY = registerKey("sativa_rare_placed");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BELDIA_PLACED_KEY = registerKey("beldia_placed");

    /** One attempt per this many desert chunks. Most land nowhere near water, which is the real rarity. */
    private static final int BELDIA_RARITY = 8;

    // Surface: 1 patch per 48 chunks in valid biomes. The wild plant is the main seed source, so this
    // is what decides how soon a player can start a farm: one find, not one per hill.
    public static final ConfiguredFeature<?, ?> INDICA_PLACED = register(INDICA_PLACED_KEY,
            surfacePatch(ModConfiguredFeatures.INDICA, 48));

    // Wild Lemon Haze, surface only, in three tiers that get rarer as the ground gets harsher (see
    // ModConfiguredFeatures for the patch densities and the biome mapping). All three are rarer than
    // indica's 1-in-48 to begin with: Lemon Haze is the better of the two strains, so finding it wild
    // should take more walking.
    //
    //   home range     1 patch /  64 chunks, 1-4 plants, 1 patch in 10 is big
    //   arid scrub     1 patch / 128 chunks, 1-3 plants
    //   badlands       1 patch / 256 chunks, a plant or none
    public static final ConfiguredFeature<?, ?> SATIVA_PLACED = register(SATIVA_PLACED_KEY,
            surfacePatch(ModConfiguredFeatures.SATIVA, 64));
    public static final ConfiguredFeature<?, ?> SATIVA_SPARSE_PLACED = register(SATIVA_SPARSE_PLACED_KEY,
            surfacePatch(ModConfiguredFeatures.SATIVA_SPARSE, 128));
    public static final ConfiguredFeature<?, ?> SATIVA_RARE_PLACED = register(SATIVA_RARE_PLACED_KEY,
            surfacePatch(ModConfiguredFeatures.SATIVA_RARE, 256));

    // Wild Beldía: an attempt every few desert chunks, but the attempt only grows where sand meets
    // water, so in practice a riverbank find. The seeds' real home is the desert temple.
    public static final ConfiguredFeature<?, ?> BELDIA_PLACED = register(BELDIA_PLACED_KEY,
            surfacePatch(ModConfiguredFeatures.BELDIA, BELDIA_RARITY));

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registry.CONFIGURED_FEATURE_REGISTRY, new ResourceLocation(Hempdustry.MOD_ID, name));
    }

    /** Forces the registrations above, the patches' first. */
    public static void registerPlacedFeatures() {
        ModConfiguredFeatures.registerConfiguredFeatures();
        Hempdustry.LOGGER.info("Registering Placed Features for " + Hempdustry.MOD_ID);
    }

    /**
     * Standard surface-vegetation placement: one attempt per {@code rarity} chunks, anywhere in the
     * chunk, on the terrain top (the motion-blocking heightmap, as the newer lines' {@code HEIGHTMAP}).
     * The outermost decorator runs first, so this reads bottom-up: chance, then square, then heightmap.
     */
    private static ConfiguredFeature<?, ?> surfacePatch(ConfiguredFeature<?, ?> patch, int rarity) {
        return patch.decorated(Features.Decorators.HEIGHTMAP_SQUARE).chance(rarity);
    }

    private static ConfiguredFeature<?, ?> register(ResourceKey<ConfiguredFeature<?, ?>> key, ConfiguredFeature<?, ?> feature) {
        return BuiltinRegistries.register(BuiltinRegistries.CONFIGURED_FEATURE, key.location(), feature);
    }
}
