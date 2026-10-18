package com.warlonmhite.hempdustry.neoforge.client;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.neoforge.NeoForgeRegistration;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.platform.client.ClientServices;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** NeoForge's client entry point; a dedicated server never loads it. */
@Mod(value = Hempdustry.MOD_ID, dist = Dist.CLIENT)
public final class HempdustryNeoForgeClient {
    public HempdustryNeoForgeClient(IEventBus modBus) {
        ((NeoForgeClientRegistration) ClientServices.CLIENT).listen(modBus);
        // The flip-flops and the tam are drawn by a layer of the mod's, in place of their armour pieces.
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> {
            event.registerItem(new OutfitExtensions(), ModItems.FLIP_FLOPS, ModItems.HEMP_BEANIE);
            // The shirt and the trousers, undyed, in unbleached hemp.
            event.registerItem(new UndyedHempExtensions(), ModItems.HEMP_SHIRT, ModItems.HEMP_HAREM_PANTS);
            // The hemp bed's item, with the bed's own renderer.
            event.registerItem(new HempBedItemExtensions(), ModBlocks.HEMP_BED.asItem());
        });
        modBus.addListener(EntityRenderersEvent.AddLayers.class, HempdustryNeoForgeClient::addOutfitLayer);
        // A sign draws its wood type's texture, which the client looks up from a table of its own.
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() ->
                ((NeoForgeRegistration) Services.REGISTRATION).woodTypes().forEach(Sheets::addWoodType)));
    }

    /** On every body drawn with a humanoid model -- every one that wears armour -- players included. */
    private static void addOutfitLayer(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            addOutfitLayer(event.getRenderer(type));
        }
        for (PlayerSkin.Model skin : event.getSkins()) {
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
