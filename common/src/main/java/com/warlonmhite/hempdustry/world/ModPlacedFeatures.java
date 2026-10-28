package com.warlonmhite.hempdustry.world;


import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.material.Fluids;
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
    /** Vanilla sugar cane's patch shape: 20 tries within 4 blocks, all on the centre's level. */
    private static final int BELDIA_TRIES = 20;

    /**
     * A wild patch is usually small and one in ten is a big one — vanilla's own 64-try flower
     * patch. Most tries land nothing: a try only counts where the offset's {@code dy} is 0 and the
     * spot is still air, and the grass vanilla has already placed takes most of the air. Measured
     * with {@code /place feature} in real terrain (on 1.21.11, when the patch was one feature), a try
     * lands about 0.1 plants in plains and less in savanna, so 24 tries gives 1-4 plants in most
     * patches and 64 gives the lucky 6-8. A single fixed count cannot give that shape: anything high
     * enough for the lucky tail makes it the norm on bare ground. So the count is drawn per patch, nine
     * small to one big — the two patches a selector chose between before 26.1.
     */
    private static final int SMALL_TRIES = 24;
    private static final int BIG_TRIES = 64;
    private static final IntProvider WILD_TRIES = new WeightedListInt(WeightedList.<IntProvider>builder()
            .add(ConstantInt.of(SMALL_TRIES), 9)
            .add(ConstantInt.of(BIG_TRIES), 1)
            .build());

    public static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name));
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configured = context.lookup(Registries.CONFIGURED_FEATURE);

        // Surface: 1 patch per 48 chunks in valid biomes. The wild plant is the main seed source,
        // so this is what decides how soon a player can start a farm: one find, not one per hill.
        register(context, INDICA_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.INDICA_KEY), wildPatch(48, WILD_TRIES));

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
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_KEY), wildPatch(64, WILD_TRIES));
        register(context, SATIVA_SPARSE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_SPARSE_KEY), wildPatch(128, ConstantInt.of(6)));
        register(context, SATIVA_RARE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_RARE_KEY), wildPatch(256, ConstantInt.of(3)));

        // Wild Beldía: an attempt every few desert chunks, but the attempt only grows where sand meets
        // water, so in practice a riverbank find. The seeds' real home is the desert temple.
        //
        // The ground test is #hempdustry:beldia_soil -- the plant's own survival rule, so a modded
        // desert's sands work -- and it is asked directly rather than through wouldSurvive, because a
        // crop's canSurvive also reads light and worldgen has none yet. The water is vanilla's own four
        // offsets from nearWaterPredicate. InBeldiaBiome then checks each plant's own biome,
        // since the patch spreads past its centre's -- see that class. A desert shore grows it too.
        register(context, BELDIA_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.BELDIA_KEY),
                RarityFilter.onAverageOnceEvery(BELDIA_RARITY),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome(),
                CountPlacement.of(BELDIA_TRIES),
                RandomOffsetPlacement.ofTriangle(4, 0),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(new Vec3i(0, 1, 0), Blocks.AIR),
                        BlockPredicate.matchesTag(new Vec3i(0, -1, 0), ModTags.Blocks.BELDIA_SOIL),
                        BlockPredicate.anyOf(
                                BlockPredicate.matchesFluids(new Vec3i(1, -1, 0), Fluids.WATER, Fluids.FLOWING_WATER),
                                BlockPredicate.matchesFluids(new Vec3i(-1, -1, 0), Fluids.WATER, Fluids.FLOWING_WATER),
                                BlockPredicate.matchesFluids(new Vec3i(0, -1, 1), Fluids.WATER, Fluids.FLOWING_WATER),
                                BlockPredicate.matchesFluids(new Vec3i(0, -1, -1), Fluids.WATER, Fluids.FLOWING_WATER)))),
                ModPlacementModifiers.InBeldiaBiomePlacementModifier.INSTANCE);
    }

    /**
     * A wild patch: one per {@code rarity} chunks, on the terrain top, then {@code tries} plants
     * scattered up to 6 blocks out and 2 up or down, each only where there is air — vanilla's
     * {@code random_patch} as it was, written out as placement the way 26.1 writes its own flowers.
     */
    private static PlacementModifier[] wildPatch(int rarity, IntProvider tries) {
        return new PlacementModifier[]{
                RarityFilter.onAverageOnceEvery(rarity),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome(),
                CountPlacement.of(tries),
                RandomOffsetPlacement.ofTriangle(6, 2),
                BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)};
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
