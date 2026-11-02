package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.loot.LootTableOrigin;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Keeps the resources the loot tables are about to be read from, so the loot event can ask which
 * pack a table came from. Neither loader's loot event can tell Mojang's experiment packs from a
 * datapack: NeoForge's does not say where a table came from, and Fabric's files the experiment packs
 * with the datapacks.
 *
 * <p>Since 26.3 the loot tables load with every other reloadable registry in one pass, so the
 * resources are kept as that pass starts.
 */
@Mixin(ReloadableServerRegistries.class)
public class ReloadableServerRegistriesMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private static void hempdustry$keepResources(LayeredRegistryAccess<RegistryLayer> context,
                                                 List<Registry.PendingTags<?>> updatedContextTags,
                                                 ResourceManager resources, Executor executor,
                                                 CallbackInfoReturnable<CompletableFuture<ReloadableServerRegistries.LoadResult>> cir) {
        LootTableOrigin.loading = resources;
    }
}
