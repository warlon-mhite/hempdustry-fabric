package com.warlonmhite.hempdustry.world.gen;

import com.warlonmhite.hempdustry.world.ModConfiguredFeatures;
import com.warlonmhite.hempdustry.world.ModPlacedFeatures;

public class ModWorldGeneration {
    public static void generateModWorldGeneration(){
        // 1.18.2 takes a mod's features from vanilla's built-in registries, filled here, before any
        // biome asks for them.
        ModConfiguredFeatures.bootstrap();
        ModPlacedFeatures.bootstrap();
        ModFlowerGeneration.generateFlowers();
    }
}
