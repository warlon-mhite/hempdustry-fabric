package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.loot.LootTableOrigin;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the registries the loot tables are about to be loaded against. The mod's loot reads the
 * world's strains, a datapack registry, and on 1.20.1 neither loader's loot event hands one over:
 * the loot tables load beside the registries rather than inside them, before any server exists.
 */
@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {
    @Inject(method = "loadResources", at = @At("HEAD"))
    private static void hempdustry$keepRegistries(ResourceManager resources, RegistryAccess.Frozen registries,
                                                  FeatureFlagSet features, Commands.CommandSelection selection,
                                                  int functionLevel, Executor background, Executor game,
                                                  CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
        LootTableOrigin.registries = registries;
    }
}
