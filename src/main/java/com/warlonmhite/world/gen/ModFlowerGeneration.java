package com.warlonmhite.world.gen;

import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.world.ModPlacedFeatures;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.gen.GenerationStep;


public class ModFlowerGeneration {
    public static void generateFlowers() {
        BiomeModifications.addFeature(
                BiomeSelectors.tag(ModTags.Biomes.INDICA_FLOWER_GEN),
                GenerationStep.Feature.VEGETAL_DECORATION,
                ModPlacedFeatures.INDICA_PLACED_KEY
        );
    }
}
