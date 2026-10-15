package com.warlonmhite.hempdustry.fabric.client;

import com.warlonmhite.hempdustry.HempdustryClient;
import com.warlonmhite.hempdustry.client.render.HempBeanieRenderer;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;

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
    }
}
