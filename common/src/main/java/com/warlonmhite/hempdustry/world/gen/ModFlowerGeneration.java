package com.warlonmhite.hempdustry.world.gen;

import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModPlacedFeatures;
import net.minecraft.world.level.levelgen.GenerationStep;


public class ModFlowerGeneration {
    public static void generateFlowers() {
        // The decorated patches go into vanilla's built-in registry now, while the mod registers, so
        // they carry their ids into every world's registries.
        ModPlacedFeatures.registerPlacedFeatures();

        // Surface biomes (plains / jungles / swamps + modded equivalents). No cave tier: the newer
        // lines' grows in lush caves, which 1.16.5 does not have.
        Services.REGISTRATION.addFeature(
                ModTags.Biomes.INDICA_FLOWER_GEN,
                GenerationStep.Decoration.VEGETAL_DECORATION,
                () -> ModPlacedFeatures.INDICA_PLACED
        );

        // Wild Lemon Haze — surface only, no cave tier: sativa is a sun plant. Three tiers thinning
        // out as the ground gets harsher. The biome selectors behind them are deliberately disjoint,
        // so a biome only ever matches one of these and can't be decorated twice.
        Services.REGISTRATION.addFeature(
                ModTags.Biomes.SATIVA_FLOWER_GEN,
                GenerationStep.Decoration.VEGETAL_DECORATION,
                () -> ModPlacedFeatures.SATIVA_PLACED
        );
        Services.REGISTRATION.addFeature(
                ModTags.Biomes.SATIVA_FLOWER_GEN_SPARSE,
                GenerationStep.Decoration.VEGETAL_DECORATION,
                () -> ModPlacedFeatures.SATIVA_SPARSE_PLACED
        );
        Services.REGISTRATION.addFeature(
                ModTags.Biomes.SATIVA_FLOWER_GEN_RARE,
                GenerationStep.Decoration.VEGETAL_DECORATION,
                () -> ModPlacedFeatures.SATIVA_RARE_PLACED
        );

        // Wild Beldía, ripe, on desert riverbanks. Deserts only -- the selector holds no beach.
        Services.REGISTRATION.addFeature(
                ModTags.Biomes.BELDIA_GEN,
                GenerationStep.Decoration.VEGETAL_DECORATION,
                () -> ModPlacedFeatures.BELDIA_PLACED
        );
    }
}
