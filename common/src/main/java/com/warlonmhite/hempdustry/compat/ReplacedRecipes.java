package com.warlonmhite.hempdustry.compat;

import com.google.gson.JsonElement;
import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Recipes another mod makes for our items at runtime that a recipe of ours replaces.
 *
 * <p>A mod that writes its recipes into a pack of its own while the game loads can end up above every
 * mod's data, so a file of ours at the same id would be shadowed rather than win. Each such recipe is
 * named here with the one of ours that does its job, and is dropped from the recipe files before
 * they are read whenever ours is among them: on server start and on every {@code /reload}, on both
 * loaders, and the clients receive the recipes already without it. Without the other mod neither
 * recipe exists and nothing changes.
 */
public final class ReplacedRecipes {

    /** Another mod's runtime recipe id, and the recipe of ours that replaces it. */
    public static final Map<ResourceLocation, ResourceLocation> REPLACED = Map.of(
            // Create 6 reads hemp_stem beside hemp_planks as a log and its planks and makes itself a saw
            // recipe turning one stem into six planks. Ours strips the stalk into its 4 fibre, as by hand.
            new ResourceLocation("create",
                    "cutting/runtime_generated/compat/create/tag_runtime_generated/compat/hempdustry/hemp_to_hemp_planks"),
            new ResourceLocation(Hempdustry.MOD_ID, "compat/create/cutting/hemp_fiber"));

    private ReplacedRecipes() {
    }

    /**
     * The recipe files without every listed recipe whose replacement is among them; the map itself when
     * there is nothing to drop.
     */
    public static Map<ResourceLocation, JsonElement> without(Map<ResourceLocation, JsonElement> recipes) {
        Map<ResourceLocation, JsonElement> kept = recipes;
        for (Map.Entry<ResourceLocation, ResourceLocation> replaced : REPLACED.entrySet()) {
            if (recipes.containsKey(replaced.getKey()) && recipes.containsKey(replaced.getValue())) {
                if (kept == recipes) {
                    kept = new HashMap<>(recipes);
                }
                kept.remove(replaced.getKey());
            }
        }
        return kept;
    }
}
