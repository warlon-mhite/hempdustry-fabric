package com.warlonmhite.hempdustry.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
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
 * Recipes for other mods' machines, each loaded only when that mod is: Farmer's Delight's cutting board, and
 * hemp as a Silent Gear material.
 *
 * <p>On Minecraft 1.21.11 those are the partners there are: Farmer's Delight comes as Refabricated, on Fabric,
 * and Silent Gear on NeoForge. Create's mixer and saw, Mekanism's crusher, MoreMachine, Botany Pots and
 * Immersive Engineering have no build for this Minecraft, so their recipes stay on the 1.21.1 line, where
 * those mods are. Every file is written in this Minecraft's recipe format (ingredients as plain ids and tags),
 * the one the partners' own 1.21.11 jars use.
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
    private static final String SILENT_GEAR = "silentgear";

    private final PackOutput output;
    private final List<CompletableFuture<?>> writes = new ArrayList<>();
    private CachedOutput cache;

    public ModCompatProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.cache = cache;
        this.writes.clear();
        farmersDelight();
        silentGear();
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

    // ----- Silent Gear -----

    private void silentGear() {
        // Hemp fibre as a gear material, beside Silent Gear's flax, string and sinew. Hemp cord is strong,
        // stretches little and wears well: as a cord it adds a little power and no draw speed (flax draws
        // faster and hits softer, sinew hits harder); as a binding it adds durability with less give than
        // flax. A sidegrade among them, not their better.
        JsonObject material = new JsonObject();
        material.addProperty("type", "silentgear:simple");
        material.addProperty("parent", "silentgear:empty");
        JsonObject crafting = new JsonObject();
        crafting.addProperty("can_salvage", true);
        JsonArray categories = new JsonArray();
        categories.add("organic");
        categories.add("fiber");
        categories.add("basic");
        crafting.add("categories", categories);
        crafting.add("gear_type_blacklist", new JsonArray());
        crafting.addProperty("ingredient", id(ModItems.HEMP_FIBER));
        crafting.add("part_substitutes", new JsonObject());
        material.add("crafting", crafting);
        JsonObject display = new JsonObject();
        display.addProperty("color", "#FF988F77");
        // Worn on armour as Silent Gear's own fibre materials are: its flat generic layer, tinted.
        JsonObject equippable = new JsonObject();
        equippable.addProperty("always_use_tint", true);
        equippable.addProperty("asset_id", "silentgear:generic_flat");
        equippable.addProperty("compound_asset_id", "silentgear:generic_flat");
        equippable.addProperty("equip_sound", "minecraft:item.armor.equip_generic");
        display.add("equippable", equippable);
        display.addProperty("main_texture_type", "LOW_CONTRAST");
        JsonObject name = new JsonObject();
        name.addProperty("translate", "material.hempdustry.hemp");
        display.add("name", name);
        display.addProperty("name_prefix", "");
        material.add("display", display);
        JsonObject binding = new JsonObject();
        binding.add("armor_durability", silentGearModifier(0.1F));
        binding.add("traits", flexible(2));
        JsonObject cord = new JsonObject();
        cord.add("ranged_damage", silentGearModifier(0.1F));
        JsonObject properties = new JsonObject();
        properties.add("silentgear:binding", binding);
        properties.add("silentgear:cord", cord);
        material.add("properties", properties);
        Path file = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(Hempdustry.MOD_ID)
                .resolve("silentgear_materials").resolve("hemp.json");
        writes.add(DataProvider.saveStable(cache, material, file));
    }

    private static JsonObject silentGearModifier(float value) {
        JsonObject modifier = new JsonObject();
        modifier.addProperty("operation", "MULTIPLY_BASE");
        modifier.addProperty("value", value);
        return modifier;
    }

    private static JsonArray flexible(int level) {
        JsonObject trait = new JsonObject();
        trait.add("conditions", new JsonArray());
        trait.addProperty("level", level);
        trait.addProperty("trait", "silentgear:flexible");
        JsonArray traits = new JsonArray();
        traits.add(trait);
        return traits;
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
