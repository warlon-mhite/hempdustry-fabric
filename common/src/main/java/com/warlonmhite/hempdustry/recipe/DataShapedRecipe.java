package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * Vanilla's shaped recipe, whose result may carry data — a spliff's load. Its file is vanilla's shaped
 * recipe with an {@code "nbt"} on the result ({@link RecipeJson#stack}): vanilla 1.20.1 reads a crafting
 * result as an item and a count only. Forge reads the {@code nbt} for vanilla's own type, Fabric does
 * not; this type reads it on both. It is a {@link ShapedRecipe} in every other respect, so the recipe
 * book and the recipe viewers show it as one.
 */
public class DataShapedRecipe extends ShapedRecipe {

    public DataShapedRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(null), base.showNotification());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DATA_SHAPED;
    }

    public static class Serializer implements RecipeSerializer<DataShapedRecipe> {
        @Override
        public DataShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new DataShapedRecipe(RecipeJson.shaped(id, json));
        }

        @Override
        public DataShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            // The packet carries the whole result stack, its tag included.
            return new DataShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DataShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
