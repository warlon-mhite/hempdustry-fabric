package com.warlonmhite.hempdustry.world;


import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placementmodifier.*;

import java.util.List;

public class ModPlacedFeatures {

    public static final RegistryKey<PlacedFeature> INDICA_PLACED_KEY = registerKey("indica_placed");
    public static final RegistryKey<PlacedFeature> INDICA_CAVE_PLACED_KEY = registerKey("indica_cave_placed");
    public static final RegistryKey<PlacedFeature> SATIVA_PLACED_KEY = registerKey("sativa_placed");
    public static final RegistryKey<PlacedFeature> SATIVA_SPARSE_PLACED_KEY = registerKey("sativa_sparse_placed");
    public static final RegistryKey<PlacedFeature> SATIVA_RARE_PLACED_KEY = registerKey("sativa_rare_placed");

    public static RegistryKey<PlacedFeature> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Hempdustry.MOD_ID, name));
    }

    public static void bootstrap(Registerable<PlacedFeature> context) {
        var configured = context.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE);

        // Surface: ~1 patch per 9 chunks in valid biomes (uncommon, find-worthy).
        register(context, INDICA_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.INDICA_KEY),
                RarityFilterPlacementModifier.of(9),
                SquarePlacementModifier.of(),
                PlacedFeatures.MOTION_BLOCKING_HEIGHTMAP,
                BiomePlacementModifier.of());

        // Caves: scatter single flowers on cave floors. Mirrors vanilla lush-caves
        // vegetation placement (scan down to a solid floor through air, then sit one
        // block above it). The Y range is the whole column (BOTTOM_TO_120_RANGE is a
        // yarn misnomer for bottom..256), and the 12-step floor scan only succeeds
        // near an actual cave floor, so most attempts miss -- vanilla moss uses count
        // 125 here. 24 keeps indica clearly sparser than moss but still findable.
        register(context, INDICA_CAVE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.INDICA_CAVE_KEY),
                CountPlacementModifier.of(24),
                SquarePlacementModifier.of(),
                PlacedFeatures.BOTTOM_TO_120_RANGE,
                EnvironmentScanPlacementModifier.of(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.IS_AIR, 12),
                RandomOffsetPlacementModifier.vertically(ConstantIntProvider.create(1)),
                BiomePlacementModifier.of());

        // Wild Lemon Haze, surface only, in three tiers that get rarer as the ground gets harsher
        // (see ModConfiguredFeatures for the patch densities and the biome mapping). All three are
        // rarer than indica's 1-in-9 to begin with: Lemon Haze is the better of the two strains, so
        // finding it wild should take more walking.
        //
        //   home range     1 patch / 12 chunks, up to 12 flowers
        //   arid scrub     1 patch / 24 chunks, up to  6 flowers
        //   badlands       1 patch / 48 chunks, up to  3 flowers
        register(context, SATIVA_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_KEY), surfacePatch(12));
        register(context, SATIVA_SPARSE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_SPARSE_KEY), surfacePatch(24));
        register(context, SATIVA_RARE_PLACED_KEY,
                configured.getOrThrow(ModConfiguredFeatures.SATIVA_RARE_KEY), surfacePatch(48));
    }

    /** Standard surface-vegetation placement: one attempt per {@code rarity} chunks, on the terrain top. */
    private static PlacementModifier[] surfacePatch(int rarity) {
        return new PlacementModifier[]{
                RarityFilterPlacementModifier.of(rarity),
                SquarePlacementModifier.of(),
                PlacedFeatures.MOTION_BLOCKING_HEIGHTMAP,
                BiomePlacementModifier.of()};
    }



    private static void register(Registerable<PlacedFeature> context, RegistryKey<PlacedFeature> key, RegistryEntry<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }

    private static <FC extends FeatureConfig, F extends Feature<FC>> void register(Registerable<PlacedFeature> context, RegistryKey<PlacedFeature> key,
                                                                                   RegistryEntry<ConfiguredFeature<?, ?>> configuration,
                                                                                   PlacementModifier... modifiers) {
        register(context, key, configuration, List.of(modifiers));
    }
}
