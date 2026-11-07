package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.block.ModBlocks;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * One squeeze in the Hemp Press: an ingredient in, whatever heat and pressure make of it out.
 *
 * <p>Deliberately the same shape as {@link DecarboxylatingRecipe} — one ingredient, one result, no
 * per-recipe time — and for the same reasons. The machine runs one clock synced to the screen
 * through a single property; a per-recipe time would mean drawing the arrow from one recipe's scale
 * while another was finishing. The speed knob is {@code world.machineSpeedMultiplier}.
 *
 * <p>Copied rather than shared with the decarboxylating recipe. The two are identical today and are
 * a plausible pair to fold together later, but an abstract base whose only job is to hand back a
 * different {@code RecipeType} would be more machinery than the duplication it removes.
 *
 * <p>Not in the recipe book: the book only knows the crafting types, and a machine recipe there
 * would be an entry the player could not click. Recipe viewers read the type directly.
 */
public record PressingRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result)
        implements Recipe<Container> {

    /** The input sits in the container's first slot. */
    @Override
    public boolean matches(Container input, Level world) {
        return ingredient.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(Container input) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.withSize(1, Ingredient.EMPTY);
        list.set(0, ingredient);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /** Never shown in the book — {@code Recipe} demands one, so this is the harmless answer. */
    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.HEMP_PRESS);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PRESSING;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.PRESSING_TYPE;
    }

    public static class Serializer extends ModRecipeSerializer<PressingRecipe> {
        @Override
        public PressingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new PressingRecipe(id, RecipeJson.ingredient(json, "ingredient"), RecipeJson.result(json, "result"));
        }

        @Override
        public PressingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new PressingRecipe(id, Ingredient.fromNetwork(buf), buf.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, PressingRecipe recipe) {
            recipe.ingredient().toNetwork(buf);
            buf.writeItem(recipe.result());
        }
    }
}
