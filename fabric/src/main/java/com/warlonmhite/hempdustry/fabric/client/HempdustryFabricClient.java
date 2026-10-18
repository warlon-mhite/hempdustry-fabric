package com.warlonmhite.hempdustry.fabric.client;

import com.warlonmhite.hempdustry.HempdustryClient;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.client.render.HempBeanieRenderer;
import com.warlonmhite.hempdustry.client.render.HempBedBlockEntityRenderer;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;

/** Fabric's client entry point. */
public class HempdustryFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HempdustryClient.init();
        // Fabric's armour hook draws the flip-flops in place of the boots layer, and the beanie as a
        // slouchy tam in place of the helmet.
        ArmorRenderer.register(new FlipFlopsRenderer(), ModItems.FLIP_FLOPS);
        ArmorRenderer.register((matrices, vertexConsumers, stack, entity, slot, light, contextModel) ->
                HempBeanieRenderer.render(matrices, vertexConsumers, stack, entity, light, contextModel), ModItems.HEMP_BEANIE);
        // Fabric's built-in item hook draws the hemp bed's item with the bed's own renderer.
        BuiltinItemRendererRegistry.INSTANCE.register(ModBlocks.HEMP_BED, (stack, mode, matrices, vertexConsumers, light, overlay) ->
                HempBedBlockEntityRenderer.renderItem(matrices, vertexConsumers, light, overlay));
    }
}
