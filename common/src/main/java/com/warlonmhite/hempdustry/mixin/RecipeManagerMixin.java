package com.warlonmhite.hempdustry.mixin;

import com.google.gson.JsonElement;
import com.warlonmhite.hempdustry.compat.ReplacedRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/**
 * Drops another mod's runtime recipe for one of our items where a recipe of ours replaces it, before
 * the recipe files are read ({@link ReplacedRecipes}). Forge reads its load conditions later in the
 * same method, Fabric before it; either way only the listed ids leave.
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @ModifyVariable(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true)
    private Map<ResourceLocation, JsonElement> hempdustry$dropReplaced(Map<ResourceLocation, JsonElement> recipes) {
        return ReplacedRecipes.without(recipes);
    }
}
