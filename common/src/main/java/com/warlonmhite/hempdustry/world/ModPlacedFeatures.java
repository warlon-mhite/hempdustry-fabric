package com.warlonmhite.hempdustry.world;


import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.util.ModTags;
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
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement;
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
    public static final ResourceKey<PlacedFeature> WARPED_KUSH_PLACED_KEY = registerKey("warped_kush_placed");

    /** One chunk in this many tries for a wild Warped Kush patch, on every floor of the Nether there. */
    private static final int WARPED_KUSH_RARITY = 8;
    public static final ResourceKey<PlacedFeature> RUDERALIS_PLACED_KEY = registerKey("ruderalis_placed");
    public static final ResourceKey<PlacedFeature> CRIMSON_PLACED_KEY = registerKey("crimson_placed");
    public static final ResourceKey<PlacedFeature> ENDER_HAZE_PLACED_KEY = registerKey("ender_haze_placed");
    public static final ResourceKey<PlacedFeature> CHERRY_WINE_PLACED_KEY = registerKey("cherry_wine_placed");

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

    /**
     * One chunk in this many tries for wild Crimson Weed, under the crimson forest's canopy — every
     * chunk, since <b>a scan is not a plant</b>: most start in netherrack, lava or open air, and the
     * weeping vines already hang under many caps. Measured in real terrain (the test server's Nether,
     * 1536 force-generated chunks round six located crimson forests, 740 of them forest), these numbers
     * place <b>one plant per 16 forest chunks</b>, level with Warped Kush's measured one per 15. At
     * rarity 4 and 8 scans it was one per 126.
     */
    private static final int CRIMSON_RARITY = 1;
    /** Attempts in such a chunk to find a canopy overhead, scanning up from a random point. */
    private static final int CRIMSON_SCANS = 6;

    /**
     * One attempt per this many taiga chunks, and <b>an attempt is not a plant</b>: measured in real
     * terrain (a snowy taiga, seed 20260922, 256 force-loaded chunks of which 101 were taiga-centred)
     * this patch lands <b>0.47 plants per attempt</b>, since most of its tries meet fern, grass or
     * sloping ground. So this number is about <b>one wild plant per 820 taiga chunks</b> — rarer than
     * anything else in the mod (Lemon Haze's badlands tier is 1 patch per 256 chunks) and rarer in
     * practice than vanilla's own 384 for a rare berry patch, which lands far more per attempt.
     *
     * <p>That is the intent: Ruderalis is meant to be <b>dug up</b>. Trail ruins and sniffers are the
     * sources a player can work towards; a wild stand is luck, worth about one find to a player who
     * crosses five or six taigas looking at the ground.
     */
    private static final int RUDERALIS_RARITY = 384;

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

        // Wild Cherry Wine: Purple Kush's surface patch, in cherry groves -- one per 48 chunks,
        // small, one in ten big. The flower keeps to its own ground, as every wild flower does.
        register(context, CHERRY_WINE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.CHERRY_WINE_KEY), surfacePatch(48));

        // Wild Beldía: an attempt every few desert chunks, but the attempt only grows where sand meets
        // water, so in practice a riverbank find. The seeds' real home is the desert temple.
        register(context, BELDIA_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.BELDIA_KEY), surfacePatch(BELDIA_RARITY));
        // Ditchweed: one attempt per 12 chunks, and the attempt only grows where there is water
        // beside dirt — so in practice a riverbank find, not a meadow one. Vanilla sugar cane tries
        // every 6 chunks with the same filter; the wild plant is a seed source and stays rarer.
        register(context, DITCHWEED_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.DITCHWEED_KEY), surfacePatch(12));

        // Warped Kush: a heightmap in the Nether only finds the bedrock roof, so this is placed the
        // way vanilla's own Nether vegetation is -- CountOnEveryLayer, one attempt on each floor of the
        // column -- after a rarity roll on the chunk.
        register(context, WARPED_KUSH_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.WARPED_KUSH_KEY),
                RarityFilter.onAverageOnceEvery(WARPED_KUSH_RARITY),
                CountOnEveryLayerPlacement.of(1),
                BiomeFilter.biome());

        // Crimson Weed: the canopy is overhead, not a heightmap surface, so this is placed the way
        // vanilla places its cave vines -- a random point in the column, a scan up through air to the
        // first canopy block, then one block down, under it. A scan that meets anything else first
        // (netherrack, a stem, roots) finds nothing.
        register(context, CRIMSON_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.CRIMSON_KEY),
                RarityFilter.onAverageOnceEvery(CRIMSON_RARITY),
                CountPlacement.of(CRIMSON_SCANS),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.matchesTag(ModTags.Blocks.CRIMSON_CANOPY),
                        BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
                BiomeFilter.biome());

        // Ender Haze: the islands' top is an ordinary surface, so an ordinary surface patch.
        register(context, ENDER_HAZE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.ENDER_HAZE_KEY), surfacePatch(ENDER_HAZE_RARITY));

        // Ruderalis: a relict stand in the cold north, where the plant is actually from and where
        // vanilla already puts the trail ruins that hold its seeds.
        register(context, RUDERALIS_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.RUDERALIS_KEY), surfacePatch(RUDERALIS_RARITY));
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
