package com.warlonmhite.hempdustry.world;


import net.minecraft.core.Registry;
import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.BuiltinRegistries;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import java.util.List;

public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> INDICA_PLACED_KEY = registerKey("indica_placed");
    public static final ResourceKey<PlacedFeature> INDICA_CAVE_PLACED_KEY = registerKey("indica_cave_placed");
    public static final ResourceKey<PlacedFeature> SATIVA_PLACED_KEY = registerKey("sativa_placed");
    public static final ResourceKey<PlacedFeature> SATIVA_SPARSE_PLACED_KEY = registerKey("sativa_sparse_placed");
    public static final ResourceKey<PlacedFeature> SATIVA_RARE_PLACED_KEY = registerKey("sativa_rare_placed");
    public static final ResourceKey<PlacedFeature> BELDIA_PLACED_KEY = registerKey("beldia_placed");

    /** One attempt per this many desert chunks. Most land nowhere near water, which is the real rarity. */
    private static final int BELDIA_RARITY = 8;

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, new ResourceLocation(Hempdustry.MOD_ID, name));
    }

    private static final Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> REGISTERED = new HashMap<>();

    /** The placed feature registered under {@code key}; {@link #bootstrap} must have run. */
    public static Holder<PlacedFeature> holder(ResourceKey<PlacedFeature> key) {
        Holder<PlacedFeature> holder = REGISTERED.get(key);
        if (holder == null) {
            throw new IllegalStateException("Placed feature " + key.location() + " is not registered yet");
        }
        return holder;
    }

    /** Registers every placed feature into vanilla's built-in registry, after the configured ones. */
    public static void bootstrap() {

        // Surface: 1 patch per 48 chunks in valid biomes. The wild plant is the main seed source,
        // so this is what decides how soon a player can start a farm: one find, not one per hill.
        register(INDICA_PLACED_KEY,
                ModConfiguredFeatures.holder(ModConfiguredFeatures.INDICA_KEY), surfacePatch(48));

        // Caves: scatter single flowers on cave floors. Mirrors vanilla lush-caves
        // vegetation placement (scan down to a solid floor through air, then sit one
        // block above it). The Y range is the whole column (BOTTOM_TO_120_RANGE is a
        // yarn misnomer for bottom..256), and the 12-step floor scan only succeeds
        // when the attempt lands in cave air near a moss floor, so almost every attempt
        // misses: measured at count 256, 23 plants across 69 lush-cave chunks, about one
        // per 770 attempts. Attempts are independent, so 64 gives about one plant per 12
        // lush-cave chunks -- close to the surface tier's density, and well under the 125
        // vanilla moss uses. (It was 24 for its first life, placed nothing, and nobody knew.)
        //
        // The scan walks down through carpet and short grass as well as air: vanilla's moss patch
        // covers most of the moss it lays with them, and a scan that stopped on the first carpet
        // found almost no bare floor. The plant takes that tuft's place. Deliberately a list and
        // not "replaceable" -- water is replaceable (a flower in the pool), and so is tall grass,
        // whose top half would be left floating.
        register(INDICA_CAVE_PLACED_KEY,
                ModConfiguredFeatures.holder(ModConfiguredFeatures.INDICA_CAVE_KEY),
                CountPlacement.of(64),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(),
                        BlockPredicate.matchesBlocks(List.of(Blocks.AIR, Blocks.MOSS_CARPET, Blocks.GRASS)), 12),
                RandomOffsetPlacement.vertical(ConstantInt.of(1)),
                BiomeFilter.biome());

        // Wild Lemon Haze, surface only, in three tiers that get rarer as the ground gets harsher
        // (see ModConfiguredFeatures for the patch densities and the biome mapping). All three are
        // rarer than indica's 1-in-48 to begin with: Lemon Haze is the better of the two strains, so
        // finding it wild should take more walking.
        //
        //   home range     1 patch /  64 chunks, 1-4 plants, 1 patch in 10 is big
        //   arid scrub     1 patch / 128 chunks, 1-3 plants
        //   badlands       1 patch / 256 chunks, a plant or none
        register(SATIVA_PLACED_KEY,
                ModConfiguredFeatures.holder(ModConfiguredFeatures.SATIVA_KEY), surfacePatch(64));
        register(SATIVA_SPARSE_PLACED_KEY,
                ModConfiguredFeatures.holder(ModConfiguredFeatures.SATIVA_SPARSE_KEY), surfacePatch(128));
        register(SATIVA_RARE_PLACED_KEY,
                ModConfiguredFeatures.holder(ModConfiguredFeatures.SATIVA_RARE_KEY), surfacePatch(256));

        // Wild Beldía: an attempt every few desert chunks, but the attempt only grows where sand meets
        // water, so in practice a riverbank find. The seeds' real home is the desert temple.
        register(BELDIA_PLACED_KEY,
                ModConfiguredFeatures.holder(ModConfiguredFeatures.BELDIA_KEY), surfacePatch(BELDIA_RARITY));
    }

    /** Standard surface-vegetation placement: one attempt per {@code rarity} chunks, on the terrain top. */
    private static PlacementModifier[] surfacePatch(int rarity) {
        return new PlacementModifier[]{
                RarityFilter.onAverageOnceEvery(rarity),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome()};
    }



    private static void register(ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        REGISTERED.put(key, BuiltinRegistries.register(BuiltinRegistries.PLACED_FEATURE, key,
                new PlacedFeature(configuration, List.copyOf(modifiers))));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(ResourceKey<PlacedFeature> key,
                                                                                   Holder<ConfiguredFeature<?, ?>> configuration,
                                                                                   PlacementModifier... modifiers) {
        register(key, configuration, List.of(modifiers));
    }
}
