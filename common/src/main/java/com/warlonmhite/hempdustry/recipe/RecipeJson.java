package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * The JSON every recipe of the mod is read from on Minecraft 1.20.1, where a recipe serializer is
 * handed the file as JSON rather than through a codec.
 *
 * <p>Ingredients are vanilla's ({@code {"item": …}}, {@code {"tag": …}} or an array of them). A result is
 * vanilla 1.20.1's {@code {"item": …, "count": …}} plus an optional {@code "nbt"}, an object or an SNBT
 * string — the form Forge's own recipes use, so a datapack author on either loader writes the same.
 */
public final class RecipeJson {

    private RecipeJson() {
    }

    /** A required, non-empty ingredient. */
    public static Ingredient ingredient(JsonObject json, String field) {
        if (!json.has(field)) {
            throw new JsonSyntaxException("Missing " + field + ", expected an ingredient");
        }
        Ingredient ingredient = Ingredient.fromJson(json.get(field), false);
        if (ingredient.isEmpty()) {
            throw new JsonSyntaxException(field + " matches nothing");
        }
        return ingredient;
    }

    public static ItemStack result(JsonObject json, String field) {
        return stack(GsonHelper.getAsJsonObject(json, field));
    }

    public static ItemStack stack(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
        Item item = BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new JsonSyntaxException("Unknown item '" + id + "'"));
        ItemStack stack = new ItemStack(item, GsonHelper.getAsInt(json, "count", 1));
        if (json.has("nbt")) {
            stack.setTag(nbt(json.get("nbt")));
        }
        return stack;
    }

    public static JsonObject toJson(ItemStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() != 1) {
            json.addProperty("count", stack.getCount());
        }
        if (stack.getTag() != null && !stack.getTag().isEmpty()) {
            json.addProperty("nbt", stack.getTag().toString());
        }
        return json;
    }

    /**
     * Vanilla's shaped recipe as its own serializer reads it, but for the result, which is read with its
     * {@code nbt} ({@link #stack}). Vanilla 1.20.1 keeps a crafting result's item and count only.
     */
    public static ShapedRecipe shaped(ResourceLocation id, JsonObject json) {
        ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
        return new ShapedRecipe(id, base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), result(json, "result"), base.showNotification());
    }

    /** As {@link #shaped}, for vanilla's shapeless recipe. */
    public static ShapelessRecipe shapeless(ResourceLocation id, JsonObject json) {
        ShapelessRecipe base = RecipeSerializer.SHAPELESS_RECIPE.fromJson(id, json);
        return new ShapelessRecipe(id, base.getGroup(), base.category(), result(json, "result"), base.getIngredients());
    }

    private static CompoundTag nbt(JsonElement element) {
        try {
            return TagParser.parseTag(element.isJsonObject() ? element.toString() : GsonHelper.convertToString(element, "nbt"));
        } catch (CommandSyntaxException e) {
            throw new JsonSyntaxException("Invalid nbt: " + e.getMessage());
        }
    }
}
