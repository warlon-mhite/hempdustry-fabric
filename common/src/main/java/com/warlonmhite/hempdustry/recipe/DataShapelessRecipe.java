package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** {@link DataShapedRecipe}'s shapeless twin: vanilla's shapeless recipe, whose result may carry data. */
public class DataShapelessRecipe extends ShapelessRecipe {

    public DataShapelessRecipe(ShapelessRecipe base) {
        super(base.getId(), base.getGroup(), base.getResultItem(), base.getIngredients());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DATA_SHAPELESS;
    }

    public static class Serializer extends ModRecipeSerializer<DataShapelessRecipe> {
        @Override
        public DataShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new DataShapelessRecipe(RecipeJson.shapeless(id, json));
        }

        @Override
        public DataShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new DataShapelessRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DataShapelessRecipe recipe) {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buf, recipe);
        }
    }
}
