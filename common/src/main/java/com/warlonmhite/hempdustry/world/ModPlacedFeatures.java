package com.warlonmhite.hempdustry.world;


import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
    public static final ResourceKey<PlacedFeature> DITCHWEED_PLACED_KEY = registerKey("ditchweed_placed");

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name));
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configured = context.lookup(Registries.CONFIGURED_FEATURE);

        // Surface: 1 patch per 48 chunks in valid biomes. The wild plant is the main seed source,
        // so this is what decides how soon a player can start a farm: one find, not one per hill.
        register(context, INDICA_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.INDICA_KEY), surfacePatch(48));

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
        register(context, INDICA_CAVE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.INDICA_CAVE_KEY),
                CountPlacement.of(64),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(),
                        BlockPredicate.matchesBlocks(Blocks.AIR, Blocks.MOSS_CARPET, Blocks.SHORT_GRASS), 12),
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
        register(context, SATIVA_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_KEY), surfacePatch(64));
        register(context, SATIVA_SPARSE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_SPARSE_KEY), surfacePatch(128));
        register(context, SATIVA_RARE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_RARE_KEY), surfacePatch(256));

        // Wild Beldía: an attempt every few desert chunks, but the attempt only grows where sand meets
        // water, so in practice a riverbank find. The seeds' real home is the desert temple.
        register(context, BELDIA_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.BELDIA_KEY), surfacePatch(BELDIA_RARITY));
        // Ditchweed: one attempt per 12 chunks, and the attempt only grows where there is water
        // beside dirt — so in practice a riverbank find, not a meadow one. Vanilla sugar cane tries
        // every 6 chunks with the same filter; the wild plant is a seed source and stays rarer.
        register(context, DITCHWEED_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.DITCHWEED_KEY), surfacePatch(12));
    }

    /** Standard surface-vegetation placement: one attempt per {@code rarity} chunks, on the terrain top. */
    private static PlacementModifier[] surfacePatch(int rarity) {
        return new PlacementModifier[]{
                RarityFilter.onAverageOnceEvery(rarity),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome()};
    }



    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                                                                   Holder<ConfiguredFeature<?, ?>> configuration,
                                                                                   PlacementModifier... modifiers) {
        register(context, key, configuration, List.of(modifiers));
    }
}
