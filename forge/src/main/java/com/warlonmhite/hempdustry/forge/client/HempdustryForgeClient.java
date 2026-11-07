package com.warlonmhite.hempdustry.forge.client;

import com.warlonmhite.hempdustry.forge.ForgeRegistration;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.platform.client.ClientServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

/** Forge's client half, started by the mod's entry point on a client only; a dedicated server never loads it. */
public final class HempdustryForgeClient {
    private HempdustryForgeClient() {
    }

    public static void init(IEventBus modBus) {
        ((ForgeClientRegistration) ClientServices.CLIENT).listen(modBus);
        // The flip-flops and the tam are drawn by a layer of the mod's, in place of their armour
        // pieces. Forge 36 has no event for adding a layer, and builds the mods' entity renderers
        // only after client setup, so the layer goes on once loading is complete; the renderers
        // are made once per game on 1.16.5, never again on a resource reload.
        modBus.addListener((FMLLoadCompleteEvent event) -> event.enqueueWork(HempdustryForgeClient::addOutfitLayers));
        // A sign draws its wood type's texture, which the client looks up from a table of its own.
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
                ((ForgeRegistration) Services.REGISTRATION).woodTypes().forEach(Sheets::addWoodType)));
    }

    /** On every body drawn with a humanoid model -- every one that wears armour -- players included. */
    private static void addOutfitLayers() {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.renderers.values().forEach(HempdustryForgeClient::addOutfitLayer);
        dispatcher.getSkinMap().values().forEach(HempdustryForgeClient::addOutfitLayer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addOutfitLayer(EntityRenderer<?> renderer) {
        if (renderer instanceof LivingEntityRenderer && ((LivingEntityRenderer) renderer).getModel() instanceof HumanoidModel) {
            LivingEntityRenderer living = (LivingEntityRenderer) renderer;
            living.addLayer(new OutfitLayer(living));
        }
    }
}
