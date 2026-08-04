package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.Strain;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;
import com.warlonmhite.hempdustry.client.UpdateChecker;
import com.warlonmhite.hempdustry.client.render.HempBoatEntityRenderer;
import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreen;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;

public class HempdustryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UpdateChecker.init();

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HEMP_PLANKS_DOOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HEMP_PLANKS_TRAPDOOR, RenderLayer.getCutout());


        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INDICA_CROP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INDICA_FLOWER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_INDICA_FLOWER, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SATIVA_CROP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SATIVA_FLOWER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_SATIVA_FLOWER, RenderLayer.getCutout());

        EntityRendererRegistry.register(ModEntities.HEMP_BOAT, ctx -> new HempBoatEntityRenderer(ctx, false));
        EntityRendererRegistry.register(ModEntities.HEMP_CHEST_BOAT, ctx -> new HempBoatEntityRenderer(ctx, true));

        HandledScreens.register(ModScreenHandlers.DECARBOXYLATOR, DecarboxylatorScreen::new);
        HandledScreens.register(ModScreenHandlers.INFUSER, InfuserScreen::new);

        registerStrainPredicate();
    }

    /**
     * The {@code hempdustry:strain} item property, which is how a single spliff/pipe/bong item shows
     * a different texture per strain now that the strain lives in a data component rather than in the
     * item id. Same mechanism vanilla uses for a bow's {@code pulling} or a crossbow's {@code charged}.
     *
     * <p>0 when nothing is loaded, otherwise the strain's index + 1. Model overrides match with
     * {@code >=}, so the datagen'd models list them ascending — see {@code ModModelProvider}.
     */
    private static void registerStrainPredicate() {
        Identifier id = Identifier.of(Hempdustry.MOD_ID, "strain");
        ClampedModelPredicateProvider provider = (stack, world, entity, seed) -> {
            Strain strain = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).primaryStrain();
            return strain == null ? 0f : strain.ordinal() + 1;
        };
        ModelPredicateProviderRegistry.register(ModItems.SPLIFF, id, provider);
        ModelPredicateProviderRegistry.register(ModItems.WOODEN_PIPE, id, provider);
        ModelPredicateProviderRegistry.register(ModItems.BONG, id, provider);
    }
}
