package com.warlonmhite.hempdustry.forge.client;

import com.warlonmhite.hempdustry.forge.ForgeRegistration;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.platform.client.ClientServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Forge's client half, started by the mod's entry point on a client only; a dedicated server never loads it. */
public final class HempdustryForgeClient {
    private HempdustryForgeClient() {
    }

    public static void init(IEventBus modBus) {
        ((ForgeClientRegistration) ClientServices.CLIENT).listen(modBus);
        // The flip-flops and the tam are drawn by a layer of the mod's, in place of their armour pieces.
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> addOutfitLayer(event));
        // A sign draws its wood type's texture, which the client looks up from a table of its own.
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
                ((ForgeRegistration) Services.REGISTRATION).woodTypes().forEach(Sheets::addWoodType)));
    }

    /**
     * On every body drawn with a humanoid model -- every one that wears armour -- players included.
     * The renderers come from the dispatcher's table, which the event wraps: Forge 47's
     * {@code getRenderer} casts to a living renderer and throws for any other kind (boats, arrows).
     */
    private static void addOutfitLayer(EntityRenderersEvent.AddLayers event) {
        Minecraft.getInstance().getEntityRenderDispatcher().renderers.values()
                .forEach(HempdustryForgeClient::addOutfitLayer);
        for (String skin : event.getSkins()) {
            addOutfitLayer(event.getSkin(skin));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addOutfitLayer(EntityRenderer<?> renderer) {
        if (renderer instanceof LivingEntityRenderer living && living.getModel() instanceof HumanoidModel) {
            living.addLayer(new OutfitLayer(living));
        }
    }
}
