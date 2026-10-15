package com.warlonmhite.hempdustry.neoforge.mixin;

import com.google.gson.JsonElement;
import com.warlonmhite.hempdustry.neoforge.NeoForgeEvents;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Keeps the resources the loot tables are about to be read from, so the loot event can ask which
 * pack a table came from. Fabric's loot event says so itself; NeoForge's does not.
 */
@Mixin(ReloadableServerRegistries.class)
public class ReloadableServerRegistriesMixin {
    @Inject(method = "scheduleElementParse", at = @At("HEAD"))
    private static void hempdustry$keepResources(LootDataType<?> type, RegistryOps<JsonElement> ops,
                                                 ResourceManager resources, Executor executor,
                                                 CallbackInfoReturnable<CompletableFuture<WritableRegistry<?>>> cir) {
        NeoForgeEvents.lootResources = resources;
    }
}
