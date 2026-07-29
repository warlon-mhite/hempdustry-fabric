package com.warlonmhite.hempdustry.world.gen;

import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModPlacedFeatures;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.gen.GenerationStep;


public class ModFlowerGeneration {
    public static void generateFlowers() {
        // Surface biomes (plains / jungles / swamps + modded equivalents).
        BiomeModifications.addFeature(
                BiomeSelectors.tag(ModTags.Biomes.INDICA_FLOWER_GEN),
                GenerationStep.Feature.VEGETAL_DECORATION,
                ModPlacedFeatures.INDICA_PLACED_KEY
        );

        // Cave biomes (lush caves + modded underground jungles): on the cave floor.
        BiomeModifications.addFeature(
                BiomeSelectors.tag(ModTags.Biomes.INDICA_FLOWER_GEN_CAVE),
                GenerationStep.Feature.UNDERGROUND_DECORATION,
                ModPlacedFeatures.INDICA_CAVE_PLACED_KEY
        );

        // Wild Lemon Haze — surface only, no cave tier: sativa is a sun plant. Three tiers thinning
        // out as the ground gets harsher. The biome tags behind them are deliberately disjoint, so
        // a biome only ever matches one of these and can't be decorated twice.
        BiomeModifications.addFeature(
                BiomeSelectors.tag(ModTags.Biomes.SATIVA_FLOWER_GEN),
                GenerationStep.Feature.VEGETAL_DECORATION,
                ModPlacedFeatures.SATIVA_PLACED_KEY
        );
        BiomeModifications.addFeature(
                BiomeSelectors.tag(ModTags.Biomes.SATIVA_FLOWER_GEN_SPARSE),
                GenerationStep.Feature.VEGETAL_DECORATION,
                ModPlacedFeatures.SATIVA_SPARSE_PLACED_KEY
        );
        BiomeModifications.addFeature(
                BiomeSelectors.tag(ModTags.Biomes.SATIVA_FLOWER_GEN_RARE),
                GenerationStep.Feature.VEGETAL_DECORATION,
                ModPlacedFeatures.SATIVA_RARE_PLACED_KEY
        );
    }
}