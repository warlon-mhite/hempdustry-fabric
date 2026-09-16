package com.warlonmhite.hempdustry.world;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.placementmodifier.AbstractConditionalPlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifierType;

public class ModPlacementModifiers {

    public static final PlacementModifierType<InBeldiaBiomePlacementModifier> IN_BELDIA_BIOME =
            Registry.register(Registries.PLACEMENT_MODIFIER_TYPE, Identifier.of(Hempdustry.MOD_ID, "in_beldia_biome"),
                    () -> InBeldiaBiomePlacementModifier.CODEC);

    public static void registerPlacementModifiers() {
        Hempdustry.LOGGER.info("Registering Placement Modifiers for " + Hempdustry.MOD_ID);
    }

    /**
     * Each wild Beldía, not just its patch, stands in {@code #hempdustry:beldia_gen}.
     *
     * <p>{@code BiomePlacementModifier} only tests the patch's <em>centre</em>, and a patch spreads four
     * blocks, so without this a plant could land across a desert's border — on the beach beside it,
     * which is the one place Beldía is kept off. A desert that runs straight into the sea is a desert,
     * and grows it on the shore (Warlon Mhite's call, 2026-09-17).
     *
     * <p>The biome is read from the exact 4×4×4 cell, <b>not through {@code getBiome}</b>, which applies
     * vanilla's zoom jitter and can answer for a neighbouring cell — a border is exactly where that
     * lies.
     */
    public static final class InBeldiaBiomePlacementModifier extends AbstractConditionalPlacementModifier {
        public static final InBeldiaBiomePlacementModifier INSTANCE = new InBeldiaBiomePlacementModifier();
        public static final MapCodec<InBeldiaBiomePlacementModifier> CODEC = MapCodec.unit(() -> INSTANCE);

        private InBeldiaBiomePlacementModifier() {
        }

        @Override
        protected boolean shouldPlace(FeaturePlacementContext context, Random random, BlockPos pos) {
            return context.getWorld().getBiomeForNoiseGen(BiomeCoords.fromBlock(pos.getX()),
                    BiomeCoords.fromBlock(pos.getY()), BiomeCoords.fromBlock(pos.getZ())).isIn(ModTags.Biomes.BELDIA_GEN);
        }

        @Override
        public PlacementModifierType<?> getType() {
            return IN_BELDIA_BIOME;
        }
    }
}
