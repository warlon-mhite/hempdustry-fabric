package com.warlonmhite.hempdustry.mixin;

import com.google.gson.JsonElement;
import com.warlonmhite.hempdustry.loot.LootTableOrigin;
import com.warlonmhite.hempdustry.registry.DataRegistries;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * What the loot event needs while the loot tables load: which pack each table came from, so the
 * mod's loot goes only into the game's and the mods' own tables, never into a datapack's (a datapack
 * that writes a table means that table as written); and the strains, which the seed pools are built
 * from. The strains are read here, from the same resources, because the mod's own reload listeners
 * run after vanilla's -- after the loot tables.
 */
@Mixin(LootTables.class)
public class LootTablesMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void hempdustry$keepResources(Map<ResourceLocation, JsonElement> tables, ResourceManager resources,
                                          ProfilerFiller profiler, CallbackInfo ci) {
        LootTableOrigin.loading = resources;
        DataRegistries.SERVER.load(resources);
    }
}
