package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import net.minecraft.util.math.BlockPos;
import com.warlonmhite.hempdustry.item.ModItemProperties;
import com.warlonmhite.hempdustry.client.item.StrainModelIndexProperty;
import com.warlonmhite.hempdustry.client.item.StrainTintSource;
import net.minecraft.client.render.item.property.numeric.NumericProperties;
import net.minecraft.client.render.item.tint.TintSourceTypes;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.client.color.world.BiomeColors;
import com.warlonmhite.hempdustry.config.HempdustryConfig;

import com.warlonmhite.hempdustry.client.UpdateChecker;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.client.render.HempBoatEntityRenderer;
import com.warlonmhite.hempdustry.client.render.HempPressBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import com.warlonmhite.hempdustry.client.render.ModEntityModelLayers;
import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreen;
import com.warlonmhite.hempdustry.screen.custom.HempPressScreen;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.BoatEntityModel;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.BlockRenderLayer;

public class HempdustryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UpdateChecker.init();

        BlockRenderLayerMap.putBlock(ModBlocks.HEMP_PLANKS_DOOR, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.HEMP_PLANKS_TRAPDOOR, BlockRenderLayer.CUTOUT);


        // Vanilla's leaves texture, holes and all — solid would fill them in with black.
        BlockRenderLayerMap.putBlock(ModBlocks.HEMP_LEAVES, BlockRenderLayer.CUTOUT);

        BlockRenderLayerMap.putBlock(ModBlocks.INDICA_CROP, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.INDICA_FLOWER, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_INDICA_FLOWER, BlockRenderLayer.CUTOUT);

        BlockRenderLayerMap.putBlock(ModBlocks.SATIVA_CROP, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.SATIVA_FLOWER, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.POTTED_SATIVA_FLOWER, BlockRenderLayer.CUTOUT);

        BlockRenderLayerMap.putBlock(ModBlocks.BELDIA_CROP, BlockRenderLayer.CUTOUT);
        // The chains the Grow Lamp hangs from are see-through between the links.
        BlockRenderLayerMap.putBlock(ModBlocks.GROW_LAMP, BlockRenderLayer.CUTOUT);

        EntityModelLayerRegistry.registerModelLayer(ModEntityModelLayers.HEMP_BOAT,
                BoatEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModEntityModelLayers.HEMP_CHEST_BOAT,
                BoatEntityModel::getChestTexturedModelData);
        EntityRendererRegistry.register(ModEntities.HEMP_BOAT, ctx -> new HempBoatEntityRenderer(ctx, false));
        EntityRendererRegistry.register(ModEntities.HEMP_CHEST_BOAT, ctx -> new HempBoatEntityRenderer(ctx, true));

        HandledScreens.register(ModScreenHandlers.DECARBOXYLATOR, DecarboxylatorScreen::new);
        HandledScreens.register(ModScreenHandlers.INFUSER, InfuserScreen::new);
        HandledScreens.register(ModScreenHandlers.HEMP_PRESS, HempPressScreen::new);

        // The press's platen and capstan move, so a renderer draws them rather than the block model.
        BlockEntityRendererFactories.register(ModBlockEntities.HEMP_PRESS, HempPressBlockEntityRenderer::new);

        registerItemModelHooks();
        registerBlockColors();
    }

    /** Neutral white: multiplied into a texel it changes nothing, which is what "no tint" means. */
    private static final int NO_TINT = 0xFFFFFF;
    /** Where a stressed plant's tint is pulled: yellowing, the grower's first sign of a sick plant. */
    private static final int STRESSED_TINT = 0xD9C35A;
    /** Where a lamp-grown plant's tint is pulled: a deeper, bluer green. */
    private static final int LAMP_GROWN_TINT = 0x3C7A56;

    /**
     * Pulls the living plants part-way towards the grass colour of the biome they stand in, so a
     * field of hemp stops fighting the ground it grows out of.
     *
     * <h2>Why part-way, and not vanilla's tint</h2>
     *
     * Grass, ferns and sugar cane hand vanilla's grass colour straight through, but they are drawn
     * as near-greyscale art <em>meant</em> to be coloured by the biome. Ours are not: the two
     * strains are told apart by their greens — Purple Kush's muted grey-green
     * ({@code #435949}, {@code #4F7460}) against Lemon Haze's bright lime ({@code #549154},
     * {@code #6BB269}) — and a full tint multiplies both down onto the same biome colour, erasing
     * the difference. Blending the tint towards white instead keeps each strain's own hue and only
     * nudges it: at the default 0.35, a sativa leaf reads {@code #478340} in plains and
     * {@code #40863D} in a jungle, and the gold pistils stay gold ({@code #C9891F} / {@code #B68C1D})
     * rather than turning olive.
     *
     * <p>The tint is applied by the <em>model</em>, not here: only faces carrying
     * {@code "tintindex": 0} ask, which is why the crop stage models parent
     * {@code minecraft:block/tinted_cross} and the flowers are datagen'd {@code TintType.TINTED}.
     * The bud, seed and flower <b>items</b> are untouched — their models declare no tint index, so
     * an item in a hand or a slot looks exactly as drawn, with no biome to ask about anyway.
     *
     * <p>Break and step particles need no work: {@code BlockDustParticle} multiplies itself by
     * {@code BlockColors.getColor(state, world, pos, 0)}, so they follow this automatically.
     *
     * <p>Returning ARGB is deliberate but, unlike the item tint above, the alpha byte is
     * <em>ignored</em> here — {@code BlockModelRenderer} reads only the three colour bytes off a
     * block tint. {@link ColorHelper#lerp} is used for the blend all the same, so the value
     * that leaves this method is a well-formed colour rather than one that only works by luck.
     */
    private static void registerBlockColors() {
        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
            // view/pos are null when a block is coloured outside a world — an inventory render. No
            // biome exists there, so the honest answer is the art as drawn.
            if (tintIndex != 0 || view == null || pos == null) {
                return NO_TINT;
            }
            return biomeTint(BiomeColors.getGrassColor(view, pos));
        }, ModBlocks.INDICA_FLOWER, ModBlocks.SATIVA_FLOWER,
                ModBlocks.POTTED_INDICA_FLOWER, ModBlocks.POTTED_SATIVA_FLOWER);
        // The crops take the same biome tint, then show the light they grew under: a lamp-grown
        // plant a shade deeper, a stressed one yellowed. No models — the tint is a multiply per quad,
        // and the record is on the LOWER, so an upper segment walks down to find it.
        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
            if (tintIndex != 0 || view == null || pos == null) {
                return NO_TINT;
            }
            // Beldía is drawn in its own colours, so it skips the biome and keeps only the light
            // cue: a desert's grass colour is a dry olive that would turn a sand-grown plant to straw
            // (and a potted one would change colour with the room it stands in).
            int tint = state.isOf(ModBlocks.BELDIA_CROP) ? NO_TINT : biomeTint(BiomeColors.getGrassColor(view, pos));
            BlockPos lower = pos;
            for (int i = 0; i < 2 && view.getBlockState(lower.down()).isOf(state.getBlock()); i++) {
                lower = lower.down();
            }
            GrowLight light = view.getBlockState(lower).get(GrowLight.PROPERTY);
            if (light == GrowLight.STRESSED) {
                return ColorHelper.lerp(0.45F, tint, STRESSED_TINT);
            }
            return light.artificial() ? ColorHelper.lerp(0.25F, tint, LAMP_GROWN_TINT) : tint;
        }, ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP, ModBlocks.BELDIA_CROP);
    }

    /**
     * {@code grassColor} blended towards white by {@code 1 - client.biomeTintStrength}.
     *
     * <p>Strength {@code 0} therefore returns pure white — a multiply by 1.0, so the config's "off"
     * costs a lerp and nothing else, and no branch is needed to honour it.
     */
    private static int biomeTint(int grassColor) {
        return ColorHelper.lerp(
                (float) HempdustryConfig.get().client().biomeTintStrength(),
                NO_TINT,
                grassColor);
    }

    /**
     * The two hooks the smoking gear's client item definitions name.
     *
     * <p>Since 1.21.4 an item's look is data — {@code assets/hempdustry/items/*.json} — and code's
     * job is only to make the pieces those files reference exist: a numeric property they can
     * dispatch on, and a tint source they can list. Everything the old
     * {@code ModelPredicateProviderRegistry} pair did is now expressed there instead, which is why
     * this mod no longer needs an accessor mixin to reach a private map.
     *
     * <p>{@code hempdustry:packed} is gone with no replacement needed: vanilla's own
     * {@code minecraft:has_component} answers "is anything loaded" exactly.
     */
    private static void registerItemModelHooks() {
        NumericProperties.ID_MAPPER.put(ModItemProperties.STRAIN, StrainModelIndexProperty.CODEC);
        TintSourceTypes.ID_MAPPER.put(ModItemProperties.STRAIN_TINT, StrainTintSource.CODEC);
    }
}
