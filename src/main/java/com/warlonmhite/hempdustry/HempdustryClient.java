package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.client.UpdateChecker;
import com.warlonmhite.hempdustry.client.render.HempBoatEntityRenderer;
import com.warlonmhite.hempdustry.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
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
    }
}
