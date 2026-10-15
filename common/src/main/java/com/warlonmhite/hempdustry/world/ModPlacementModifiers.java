package com.warlonmhite.hempdustry.world;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class ModPlacementModifiers {

    public static final PlacementModifierType<InBeldiaBiomePlacementModifier> IN_BELDIA_BIOME =
            Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "in_beldia_biome"),
                    () -> InBeldiaBiomePlacementModifier.CODEC);

    public static void registerPlacementModifiers() {
        Hempdustry.LOGGER.info("Registering Placement Modifiers for " + Hempdustry.MOD_ID);
    }

    /**
     * Each wild Beldía, not just its patch, stands in {@code #hempdustry:beldia_gen}.
     *
     * <p>{@code BiomeFilter} only tests the patch's <em>centre</em>, and a patch spreads four
     * blocks, so without this a plant could land across a desert's border — on the beach beside it,
     * which is the one place Beldía is kept off. A desert that runs straight into the sea is a desert,
     * and grows it on the shore (Warlon Mhite's call, 2026-09-17).
     *
     * <p>The biome is read from the exact 4×4×4 cell, <b>not through {@code getBiome}</b>, which applies
     * vanilla's zoom jitter and can answer for a neighbouring cell — a border is exactly where that
     * lies.
     */
    public static final class InBeldiaBiomePlacementModifier extends PlacementFilter {
        public static final InBeldiaBiomePlacementModifier INSTANCE = new InBeldiaBiomePlacementModifier();
        public static final MapCodec<InBeldiaBiomePlacementModifier> CODEC = MapCodec.unit(() -> INSTANCE);

        private InBeldiaBiomePlacementModifier() {
        }

        @Override
        protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
            return context.getLevel().getNoiseBiome(QuartPos.fromBlock(pos.getX()),
                    QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())).is(ModTags.Biomes.BELDIA_GEN);
        }

        @Override
        public PlacementModifierType<?> type() {
            return IN_BELDIA_BIOME;
        }
    }
}
