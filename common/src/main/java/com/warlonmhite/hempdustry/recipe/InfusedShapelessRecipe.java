package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * The shapeless twin of {@link InfusedShapedRecipe} — carries cannabutter's potency and quality onto
 * its output, and is otherwise an ordinary shapeless recipe so it stays recipe-book and JEI visible.
 */
public class InfusedShapelessRecipe extends ShapelessRecipe {
    private final int offset;

    /**
     * Vanilla's shapeless recipe with an offset: the file is vanilla's shapeless recipe plus an
     * {@code "offset"}, so vanilla's serializer reads everything but that one field.
     */
    public InfusedShapelessRecipe(ShapelessRecipe base, int offset) {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        return Infusion.transfer(input, super.assemble(input, registries), offset);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSED_SHAPELESS;
    }

    public static class Serializer implements RecipeSerializer<InfusedShapelessRecipe> {
        @Override
        public InfusedShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new InfusedShapelessRecipe(RecipeJson.shapeless(id, json), GsonHelper.getAsInt(json, "offset", 0));
        }

        @Override
        public InfusedShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new InfusedShapelessRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buf), buf.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, InfusedShapelessRecipe recipe) {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buf, recipe);
            buf.writeVarInt(recipe.offset);
        }
    }
}
