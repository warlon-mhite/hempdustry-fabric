package com.warlonmhite.hempdustry.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Recipes for other mods' machines, each loaded only when that mod is: Farmer's Delight's cutting board.
 *
 * <p>On Minecraft 26.3 that is the partner there is, as Refabricated, on Fabric. Create's mixer and saw on
 * NeoForge, Mekanism's crusher, MoreMachine, Botany Pots, Immersive Engineering and Silent Gear have no build
 * for this Minecraft, so their recipes stay on the 1.21.1 line, where
 * those mods are. Every file is written in this Minecraft's recipe format (ingredients as plain ids and tags),
 * the one the partner's own 26.3 jar uses.
 *
 * <p>Every file carries both loaders' conditions, {@code neoforge:conditions} and {@code fabric:load_conditions};
 * each loader reads its own key and passes over the other's, so one file serves both jars, and without the
 * other mod the recipe is never parsed at all (its recipe type would not exist).
 *
 * <p>A machine route is a trade, never better on every count than our own chain, and nothing here activates
 * anything: heat does that, in the Decarboxylator.
 */
public class ModCompatProvider implements DataProvider {
    private static final String FARMERS_DELIGHT = "farmersdelight";

    private final PackOutput output;
    private final List<CompletableFuture<?>> writes = new ArrayList<>();
    private CachedOutput cache;

    public ModCompatProvider(FabricPackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.cache = cache;
        this.writes.clear();
        farmersDelight();
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Compat recipes";
    }

    // ----- Farmer's Delight -----

    private void farmersDelight() {
        // Stripping a stalk on the cutting board: its fibre, as by hand, and now and then the woody core
        // that is left, as a stick.
        cutting("hemp_fiber", ModItems.HEMP_STEM, 4);
        // A retted stalk on the board is scutching by hand: its 6 fibre, as from the grid, and the
        // shives -- the woody core the fibre is beaten free of -- now and then as a stick.
        cutting("hemp_fiber_from_retted_hemp_stem", ModItems.RETTED_HEMP_STEM, 6);
    }

    /**
     * A stalk on Farmer's Delight's cutting board, cut with a knife into {@code fiber} fibre and, at one in four,
     * a stick. The knife is the {@code c:} knife tag, as Farmer's Delight writes its own cutting recipes for this
     * Minecraft.
     */
    private void cutting(String name, ItemLike stalk, int fiber) {
        JsonObject recipe = conditioned(FARMERS_DELIGHT, "farmersdelight:cutting");
        JsonArray ingredients = new JsonArray();
        ingredients.add(id(stalk));
        recipe.add("ingredients", ingredients);
        JsonArray result = new JsonArray();
        result.add(farmersDelightResult(ModItems.HEMP_FIBER, fiber, 1.0F));
        result.add(farmersDelightResult("minecraft:stick", 1, 0.25F));
        recipe.add("result", result);
        recipe.addProperty("tool", "#c:tools/knife");
        write(FARMERS_DELIGHT, "cutting/" + name, recipe);
    }

    private static JsonObject farmersDelightResult(Object item, int count, float chance) {
        JsonObject stack = new JsonObject();
        stack.addProperty("count", count);
        stack.addProperty("id", id(item));
        JsonObject entry = new JsonObject();
        entry.add("item", stack);
        if (chance < 1.0F) {
            entry.addProperty("chance", chance);
        }
        return entry;
    }

    // ----- the JSON -----

    /** A recipe of {@code type}, loaded only with {@code modId}, on either loader. */
    private static JsonObject conditioned(String modId, String type) {
        JsonObject recipe = new JsonObject();
        recipe.add("neoforge:conditions", neoForgeWaitsFor(modId));
        recipe.add("fabric:load_conditions", fabricWaitsFor(modId));
        recipe.addProperty("type", type);
        return recipe;
    }

    private static JsonArray neoForgeWaitsFor(String modId) {
        JsonArray neo = new JsonArray();
        JsonObject neoLoaded = new JsonObject();
        neoLoaded.addProperty("type", "neoforge:mod_loaded");
        neoLoaded.addProperty("modid", modId);
        neo.add(neoLoaded);
        return neo;
    }

    private static JsonArray fabricWaitsFor(String modId) {
        JsonArray fabric = new JsonArray();
        JsonObject fabricLoaded = new JsonObject();
        fabricLoaded.addProperty("condition", "fabric:all_mods_loaded");
        JsonArray values = new JsonArray();
        values.add(modId);
        fabricLoaded.add("values", values);
        fabric.add(fabricLoaded);
        return fabric;
    }

    private static String id(Object item) {
        if (item instanceof String id) {
            return id;
        }
        return BuiltInRegistries.ITEM.getKey(((ItemLike) item).asItem()).toString();
    }

    /** Writes {@code recipe} as {@code hempdustry:compat/<mod>/<path>}. */
    private void write(String modId, String path, JsonElement recipe) {
        Path file = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(Hempdustry.MOD_ID)
                .resolve("recipe").resolve("compat").resolve(modId).resolve(path + ".json");
        writes.add(DataProvider.saveStable(cache, recipe, file));
    }
}
