package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.loot.LootTableOrigin;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;

/**
 * Keeps the resources the loot tables are about to be read from, so the loot event can ask which
 * pack a table came from. Neither loader's loot event can tell a datapack from the game's own packs:
 * Forge's does not say where a table came from, and Fabric's files the feature packs with the
 * datapacks.
 */
@Mixin(LootDataManager.class)
public class LootDataManagerMixin {
    @Inject(method = "scheduleElementParse", at = @At("HEAD"))
    private static void hempdustry$keepResources(LootDataType<?> type, ResourceManager resources, Executor executor,
                                                 Map<LootDataType<?>, Map<ResourceLocation, ?>> collector,
                                                 CallbackInfoReturnable<CompletableFuture<?>> cir) {
        LootTableOrigin.loading = resources;
    }
}
