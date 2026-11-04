package com.warlonmhite.hempdustry.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.recipe.RecipeJson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

/**
 * Recipes the vanilla builders cannot write, written as JSON for 1.20.1: a recipe serializer there
 * reads a file but cannot write one, so each shape the mod needs is spelled out here, in the very
 * fields its serializer reads ({@code RecipeJson}).
 */
final class RecipeExport {

    private RecipeExport() {
    }

    /** A shaped grid: each row a string of key characters, a space for an empty slot. */
    record Pattern(Map<Character, Ingredient> key, List<String> rows) {
        static Pattern of(Map<Character, Ingredient> key, String... rows) {
            return new Pattern(new LinkedHashMap<>(key), List.of(rows));
        }

        int width() {
            return rows.get(0).length();
        }

        int height() {
            return rows.size();
        }

        /** The grid's ingredients row by row, as vanilla's {@code ShapedRecipe} holds them. */
        NonNullList<Ingredient> ingredients() {
            NonNullList<Ingredient> out = NonNullList.withSize(width() * height(), Ingredient.EMPTY);
            for (int y = 0; y < height(); y++) {
                for (int x = 0; x < width(); x++) {
                    char c = rows.get(y).charAt(x);
                    out.set(y * width() + x, c == ' ' ? Ingredient.EMPTY : key.get(c));
                }
            }
            return out;
        }

        void write(JsonObject json) {
            JsonArray pattern = new JsonArray();
            rows.forEach(pattern::add);
            json.add("pattern", pattern);
            JsonObject keys = new JsonObject();
            key.forEach((c, ingredient) -> keys.add(String.valueOf(c), ingredient.toJson()));
            json.add("key", keys);
        }
    }

    /**
     * The unlock advancement every crafting recipe of the mod carries, as vanilla's builders make it:
     * the recipe is given on {@code criterion}, or on already knowing it.
     */
    static Advancement.Builder unlock(ResourceLocation recipeId, String criterionName, CriterionTriggerInstance criterion) {
        return Advancement.Builder.advancement()
                .parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion(criterionName, criterion)
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(RequirementsStrategy.OR);
    }

    static void shaped(Consumer<FinishedRecipe> exporter, RecipeSerializer<?> type, ResourceLocation id,
                       String group, Pattern pattern, ItemStack result,
                       Advancement.Builder advancement, String folder, Consumer<JsonObject> extra) {
        exporter.accept(new Finished(id, type, json -> {
            crafting(json, group);
            pattern.write(json);
            json.add("result", RecipeJson.toJson(result));
            extra.accept(json);
        }, advancement, id.withPrefix("recipes/" + folder + "/")));
    }

    static void shapeless(Consumer<FinishedRecipe> exporter, RecipeSerializer<?> type, ResourceLocation id,
                          String group, List<Ingredient> ingredients, ItemStack result,
                          Advancement.Builder advancement, String folder, Consumer<JsonObject> extra) {
        exporter.accept(new Finished(id, type, json -> {
            crafting(json, group);
            JsonArray array = new JsonArray();
            ingredients.forEach(ingredient -> array.add(ingredient.toJson()));
            json.add("ingredients", array);
            json.add("result", RecipeJson.toJson(result));
            extra.accept(json);
        }, advancement, id.withPrefix("recipes/" + folder + "/")));
    }

    /** A machine recipe: not in the recipe book, so no advancement could reveal it. */
    static void machine(Consumer<FinishedRecipe> exporter, RecipeSerializer<?> type, ResourceLocation id,
                        Consumer<JsonObject> body) {
        exporter.accept(new Finished(id, type, body, null, null));
    }

    static JsonObject ingredient(Ingredient ingredient) {
        return ingredient.toJson().getAsJsonObject();
    }

    private static void crafting(JsonObject json, String group) {
        if (!group.isEmpty()) {
            json.addProperty("group", group);
        }
        json.addProperty("category", CraftingBookCategory.MISC.getSerializedName());
    }

    private record Finished(ResourceLocation id, RecipeSerializer<?> type, Consumer<JsonObject> body,
                            @Nullable Advancement.Builder advancement,
                            @Nullable ResourceLocation advancementId) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            body.accept(json);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return type;
        }

        @Override
        public @Nullable JsonObject serializeAdvancement() {
            return advancement == null ? null : advancement.serializeToJson();
        }

        @Override
        public @Nullable ResourceLocation getAdvancementId() {
            return advancementId;
        }
    }

    /** The ingredients of a shapeless recipe, in order. */
    static List<Ingredient> list(Ingredient... ingredients) {
        return new ArrayList<>(List.of(ingredients));
    }

    static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
