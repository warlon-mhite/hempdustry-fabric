package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.block.ModBlocks;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
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
 * One tray-load in the Decarboxylator: an ingredient in, a stack of decarboxylated hemp out.
 *
 * <p><b>The machine used to compare item identity in Java</b> — indica buds, sativa buds, hemp leaf,
 * and nothing else could ever be added. That was the one place the mod's own extensibility stopped
 * dead: a third-party strain could get all the way to the oven through the strain registry and the
 * tags, and then had nowhere to go. A recipe type is the fix, and it buys three more things on the
 * way past — KubeJS and CraftTweaker can rebalance the chain, a recipe viewer can show it, and a
 * datapack can change a yield.
 *
 * <p><b>No cook time here, deliberately.</b> The oven runs one clock for all three trays and syncs it
 * to the screen through a single property; a per-recipe time would need three more properties and
 * would let one tray finish while the arrow beside it is drawn from another recipe's scale. The speed
 * knob is {@code world.machineSpeedMultiplier} in the config, which moves all three together.
 *
 * <p>Not in the recipe book: the book only knows the crafting types, and a machine recipe there would
 * be an entry the player could not click. Recipe viewers read the type directly.
 */
public record DecarboxylatingRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result)
        implements Recipe<Container> {

    /** The input sits in the container's first slot. */
    @Override
    public boolean matches(Container input, Level world) {
        return ingredient.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess registries) {
        return result.copy();
    }

    /** No grid, so nothing to fit into. */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
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

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.DECARBOXYLATOR);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DECARBOXYLATING;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.DECARBOXYLATING_TYPE;
    }

    public static class Serializer implements RecipeSerializer<DecarboxylatingRecipe> {
        @Override
        public DecarboxylatingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new DecarboxylatingRecipe(id, RecipeJson.ingredient(json, "ingredient"), RecipeJson.result(json, "result"));
        }

        @Override
        public DecarboxylatingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new DecarboxylatingRecipe(id, Ingredient.fromNetwork(buf), buf.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DecarboxylatingRecipe recipe) {
            recipe.ingredient().toNetwork(buf);
            buf.writeItem(recipe.result());
        }
    }
}
