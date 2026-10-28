package com.warlonmhite.hempdustry.neoforge.client;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.neoforge.NeoForgeRecipes;
import com.warlonmhite.hempdustry.platform.client.ClientServices;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge's client entry point; a dedicated server never loads it. */
@Mod(value = Hempdustry.MOD_ID, dist = Dist.CLIENT)
public final class HempdustryNeoForgeClient {
    public HempdustryNeoForgeClient(IEventBus modBus) {
        ((NeoForgeClientRegistration) ClientServices.CLIENT).listen(modBus);
        NeoForge.EVENT_BUS.addListener(RecipesReceivedEvent.class, event -> NeoForgeRecipes.received(event.getRecipeMap()));
        // NeoForge's armour hook draws the flip-flops in place of the boots layer.
        modBus.addListener(RegisterClientExtensionsEvent.class,
                event -> event.registerItem(new FlipFlopsExtensions(), ModItems.FLIP_FLOPS));
    }
}
