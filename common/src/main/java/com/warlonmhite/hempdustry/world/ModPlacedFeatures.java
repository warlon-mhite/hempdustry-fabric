package com.warlonmhite.hempdustry.world;


import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.EnderHazeCropBlock;
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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
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
    public static final ResourceKey<PlacedFeature> DITCHWEED_PLACED_KEY = registerKey("ditchweed_placed");
    public static final ResourceKey<PlacedFeature> WARPED_KUSH_PLACED_KEY = registerKey("warped_kush_placed");
    public static final ResourceKey<PlacedFeature> RUDERALIS_PLACED_KEY = registerKey("ruderalis_placed");
    public static final ResourceKey<PlacedFeature> CRIMSON_PLACED_KEY = registerKey("crimson_placed");
    public static final ResourceKey<PlacedFeature> ENDER_HAZE_PLACED_KEY = registerKey("ender_haze_placed");
    public static final ResourceKey<PlacedFeature> CHERRY_WINE_PLACED_KEY = registerKey("cherry_wine_placed");
    public static final ResourceKey<PlacedFeature> WHITE_WIDOW_PLACED_KEY = registerKey("white_widow_placed");

    /**
     * One attempt per this many outer-island chunks for a wild Ender Haze patch, on the island's top.
     * Rarer than the Nether's wild plants on purpose: the islands are enormous and a player flies
     * over all of them, and a five-tall purple column is a landmark seen from far off. Measured in
     * real terrain (the test server's End, 2304 force-generated chunks in four squares on the outer
     * islands, 1203 of them centred in an outer-island biome), this places <b>one plant per 25 island
     * chunks</b>, in patches of two or three, every one whole — against the Nether plants' one per 15
     * or 16. At 24 it was one per 8.
     */
    private static final int ENDER_HAZE_RARITY = 64;
    /** A handful of plants on one stretch of an island: tries within three blocks, on the centre's level or one off. */
    private static final int ENDER_HAZE_TRIES = 8;

    /** One attempt per this many desert chunks. Most land nowhere near water, which is the real rarity. */
    private static final int BELDIA_RARITY = 8;
    /** Vanilla sugar cane's patch shape: 20 tries within 4 blocks, all on the centre's level. */
    private static final int BELDIA_TRIES = 20;
    /** One attempt per this many chunks: rarer than sugar cane's 6, as the wild plant is a seed source. */
    private static final int DITCHWEED_RARITY = 12;
    /** Vanilla sugar cane's patch shape: 20 tries within 4 blocks, all on the centre's level. */
    private static final int DITCHWEED_TRIES = 20;
    /** One chunk in this many tries for a wild Warped Kush patch, on every floor of the Nether there. */
    private static final int WARPED_KUSH_RARITY = 8;
    /** A Warped Kush patch: 8 tries within 3 blocks and 1 up or down. */
    private static final int WARPED_KUSH_TRIES = 8;

    /**
     * One attempt per this many taiga chunks, and <b>an attempt is not a plant</b>: measured in real
     * terrain (a snowy taiga, seed 20260922, 256 force-loaded chunks of which 101 were taiga-centred,
     * on 1.21.11) this patch lands <b>0.47 plants per attempt</b>, since most of its tries meet fern,
     * grass or sloping ground. So this number is about <b>one wild plant per 820 taiga chunks</b> —
     * rarer than anything else in the mod (Lemon Haze's badlands tier is 1 patch per 256 chunks) and
     * rarer in practice than vanilla's own 384 for a rare berry patch, which lands far more per attempt.
     *
     * <p>That is the intent: Ruderalis is meant to be <b>dug up</b>. Trail ruins and sniffers are the
     * sources a player can work towards; a wild stand is luck, worth about one find to a player who
     * crosses five or six taigas looking at the ground.
     */
    private static final int RUDERALIS_RARITY = 384;
    /** A relict stand is a weed or two, never a field: a handful of tries within two blocks. */
    private static final int RUDERALIS_TRIES = 6;

    /**
     * One chunk in this many tries for wild Crimson Weed, under the crimson forest's canopy — every
     * chunk, since <b>a scan is not a plant</b>: most start in netherrack, lava or open air, and the
     * weeping vines already hang under many caps. Measured in real terrain on 1.21.11 (the test
     * server's Nether, 1536 force-generated chunks round six located crimson forests, 740 of them
     * forest), these numbers place <b>one plant per 16 forest chunks</b>, level with Warped Kush's
     * measured one per 15. At rarity 4 and 8 scans it was one per 126.
     */
    private static final int CRIMSON_RARITY = 1;
    /** Attempts in such a chunk to find a canopy overhead, scanning up from a random point. */
    private static final int CRIMSON_SCANS = 6;
    /** A few plants under one cap of a huge fungus: tries within three blocks, most of them misses. */
    private static final int CRIMSON_TRIES = 12;

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
        var features = context.lookup(Registries.FEATURE);

        // Surface: 1 patch per 48 chunks in valid biomes. The wild plant is the main seed source,
        // so this is what decides how soon a player can start a farm: one find, not one per hill.
        register(context, INDICA_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.INDICA_KEY), wildPatch(48, WILD_TRIES));

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
                features.getOrThrow(ModConfiguredFeatures.INDICA_CAVE_KEY),
                CountPlacement.of(64),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(),
                        BlockPredicate.matchesBlocks(Blocks.AIR, Blocks.MOSS_CARPET, Blocks.SHORT_GRASS), 12),
                OffsetPlacement.vertical(ConstantInt.of(1)),
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
                features.getOrThrow(ModConfiguredFeatures.SATIVA_KEY), wildPatch(64, WILD_TRIES));
        register(context, SATIVA_SPARSE_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.SATIVA_SPARSE_KEY), wildPatch(128, ConstantInt.of(6)));
        register(context, SATIVA_RARE_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.SATIVA_RARE_KEY), wildPatch(256, ConstantInt.of(3)));

        // Wild Cherry Wine: Purple Kush's surface patch, in cherry groves -- one per 48 chunks,
        // small, one in ten big. The flower keeps to its own ground, as every wild flower does.
        register(context, CHERRY_WINE_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.CHERRY_WINE_KEY), wildPatch(48, WILD_TRIES));

        // Wild White Widow: Purple Kush's surface patch, in pale gardens -- but one per 16 chunks, not
        // 48, since a pale garden is small and rare and a 48 would leave most of them bare. Vanilla's
        // own eyeblossom patch there is one in 32.
        register(context, WHITE_WIDOW_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.WHITE_WIDOW_KEY), wildPatch(16, WILD_TRIES));

        // Wild Beldía: an attempt every few desert chunks, but the attempt only grows where sand meets
        // water, so in practice a riverbank find. The seeds' real home is the desert temple.
        //
        // The ground test is #hempdustry:beldia_soil -- the plant's own survival rule, so a modded
        // desert's sands work -- and it is asked directly rather than through wouldSurvive, because a
        // crop's canSurvive also reads light and worldgen has none yet. The water is vanilla's own four
        // offsets from nearWaterPredicate. InBeldiaBiome then checks each plant's own biome,
        // since the patch spreads past its centre's -- see that class. A desert shore grows it too.
        register(context, BELDIA_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.BELDIA_KEY),
                RarityFilter.onAverageOnceEvery(BELDIA_RARITY),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome(),
                CountPlacement.of(BELDIA_TRIES),
                OffsetPlacement.ofTriangle(4, 0),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(new Vec3i(0, 1, 0), List.of(Blocks.AIR)),
                        BlockPredicate.matchesTag(new Vec3i(0, -1, 0), ModTags.Blocks.BELDIA_SOIL),
                        BlockPredicate.anyOf(
                                BlockPredicate.matchesFluids(new Vec3i(1, -1, 0), List.of(Fluids.WATER, Fluids.FLOWING_WATER)),
                                BlockPredicate.matchesFluids(new Vec3i(-1, -1, 0), List.of(Fluids.WATER, Fluids.FLOWING_WATER)),
                                BlockPredicate.matchesFluids(new Vec3i(0, -1, 1), List.of(Fluids.WATER, Fluids.FLOWING_WATER)),
                                BlockPredicate.matchesFluids(new Vec3i(0, -1, -1), List.of(Fluids.WATER, Fluids.FLOWING_WATER))))),
                ModPlacementModifiers.InBeldiaBiomePlacementModifier.INSTANCE);

        // Ditchweed: one attempt per 12 chunks, and the attempt only grows where there is water
        // beside dirt — so in practice a riverbank find, not a meadow one. Placed exactly as vanilla
        // places sugar cane (patch_sugar_cane): each plant must survive on its ground, with water
        // beside that ground, which is why most tries of a patch land nothing.
        register(context, DITCHWEED_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.DITCHWEED_KEY),
                RarityFilter.onAverageOnceEvery(DITCHWEED_RARITY),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome(),
                CountPlacement.of(DITCHWEED_TRIES),
                OffsetPlacement.ofTriangle(4, 0),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(ModBlocks.DITCHWEED),
                        BlockPredicate.anyOf(
                                BlockPredicate.matchesFluids(new Vec3i(1, -1, 0), List.of(Fluids.WATER, Fluids.FLOWING_WATER)),
                                BlockPredicate.matchesFluids(new Vec3i(-1, -1, 0), List.of(Fluids.WATER, Fluids.FLOWING_WATER)),
                                BlockPredicate.matchesFluids(new Vec3i(0, -1, 1), List.of(Fluids.WATER, Fluids.FLOWING_WATER)),
                                BlockPredicate.matchesFluids(new Vec3i(0, -1, -1), List.of(Fluids.WATER, Fluids.FLOWING_WATER))))));

        // Warped Kush: a heightmap in the Nether only finds the bedrock roof, so this is placed the
        // way vanilla's own Nether vegetation is -- CountOnEveryLayer, one attempt on each floor of the
        // column -- after a rarity roll on the chunk. Then the patch: the tries scatter round each
        // floor, and each wants warped nylium under it and air for both halves, so a column is never
        // cut short to a lone half.
        register(context, WARPED_KUSH_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.WARPED_KUSH_KEY),
                RarityFilter.onAverageOnceEvery(WARPED_KUSH_RARITY),
                CountOnEveryLayerPlacement.of(1),
                BiomeFilter.biome(),
                CountPlacement.of(WARPED_KUSH_TRIES),
                OffsetPlacement.ofTriangle(3, 1),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(new Vec3i(0, 1, 0), List.of(Blocks.AIR)),
                        BlockPredicate.matchesBlocks(new Vec3i(0, -1, 0), List.of(Blocks.WARPED_NYLIUM)))));

        // Crimson Weed: the canopy is overhead, not a heightmap surface, so this is placed the way
        // vanilla places its cave vines -- a random point in the column, a scan up through air to the
        // first canopy block, then one block down, under it. A scan that meets anything else first
        // (netherrack, a stem, roots) finds nothing.
        register(context, CRIMSON_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.CRIMSON_KEY),
                RarityFilter.onAverageOnceEvery(CRIMSON_RARITY),
                CountPlacement.of(CRIMSON_SCANS),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.matchesTag(ModTags.Blocks.CRIMSON_CANOPY),
                        BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                OffsetPlacement.vertical(ConstantInt.of(-1)),
                BiomeFilter.biome(),
                // Then the patch: a few plants under one cap. Each wants a canopy block over its root
                // and air for both halves, so a column is never cut short to a lone half -- and never
                // hangs under a warped wart block, netherrack or anything else a live world would pop
                // it off but chunk generation would not.
                CountPlacement.of(CRIMSON_TRIES),
                OffsetPlacement.ofTriangle(3, 1),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(new Vec3i(0, -1, 0), List.of(Blocks.AIR)),
                        BlockPredicate.matchesTag(new Vec3i(0, 1, 0), ModTags.Blocks.CRIMSON_CANOPY))));

        // Ender Haze: the islands' top is an ordinary surface, so an ordinary surface patch.
        // Then the patch: a handful of plants on one stretch of an island, each wanting its soil under
        // the root and air for all five segments, so a column is never cut short -- chunk generation
        // runs no shape updates, so nothing would pop a short or misplaced plant afterwards.
        List<BlockPredicate> enderHazeRoom = new java.util.ArrayList<>(List.of(
                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesTag(new Vec3i(0, -1, 0), ModTags.Blocks.ENDER_HAZE_SOIL)));
        for (int segment = 1; segment < EnderHazeCropBlock.HEIGHT; segment++) {
            enderHazeRoom.add(BlockPredicate.matchesBlocks(new Vec3i(0, segment, 0), List.of(Blocks.AIR)));
        }
        register(context, ENDER_HAZE_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.ENDER_HAZE_KEY),
                RarityFilter.onAverageOnceEvery(ENDER_HAZE_RARITY),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome(),
                CountPlacement.of(ENDER_HAZE_TRIES),
                OffsetPlacement.ofTriangle(3, 1),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(enderHazeRoom)));

        // Ruderalis: a relict stand in the cold north, where the plant is actually from and where
        // vanilla already puts the trail ruins that hold its seeds. Each try wants air — a taiga is
        // full of ferns, berry bushes and snow layers it must not write over — and ground the plant
        // would survive on.
        register(context, RUDERALIS_PLACED_KEY,
                features.getOrThrow(ModConfiguredFeatures.RUDERALIS_KEY),
                RarityFilter.onAverageOnceEvery(RUDERALIS_RARITY),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome(),
                CountPlacement.of(RUDERALIS_TRIES),
                OffsetPlacement.ofTriangle(2, 1),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(ModBlocks.RUDERALIS_CROP))));
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
                OffsetPlacement.ofTriangle(6, 2),
                BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)};
    }



    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> feature,
                                 PlacementModifier... modifiers) {
        context.register(key, new PlacedFeature(feature, List.of(modifiers)));
    }
}
