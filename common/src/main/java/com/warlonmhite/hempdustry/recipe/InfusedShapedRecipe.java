package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A shaped recipe that carries cannabutter's potency and quality onto its output — see
 * {@link Infusion}. Ordinary in every other respect, and deliberately a {@link ShapedRecipe} subclass
 * rather than a special recipe so it stays visible in the recipe book and in JEI/EMI.
 *
 * <p>{@code offset} is the edible's own step on the potency ladder: {@code +1} concentrates,
 * {@code 0} is neutral, {@code -1} spreads the butter thin.
 */
public class InfusedShapedRecipe extends ShapedRecipe {
    private final int offset;

    /**
     * Vanilla's shaped recipe with an offset: the file is vanilla's shaped recipe plus an
     * {@code "offset"}, so vanilla's serializer reads everything but that one field.
     */
    public InfusedShapedRecipe(ShapedRecipe base, int offset) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(null), base.showNotification());
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
        return ModRecipes.INFUSED_SHAPED;
    }

    public static class Serializer implements RecipeSerializer<InfusedShapedRecipe> {
        @Override
        public InfusedShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new InfusedShapedRecipe(RecipeJson.shaped(id, json), GsonHelper.getAsInt(json, "offset", 0));
        }

        @Override
        public InfusedShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new InfusedShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf), buf.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, InfusedShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
            buf.writeVarInt(recipe.offset);
        }
    }
}
