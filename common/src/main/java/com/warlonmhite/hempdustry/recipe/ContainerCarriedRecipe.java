package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A shapeless recipe whose ingredients' containers <em>travel into the result</em> instead of being
 * handed back — i.e. one that returns no recipe remainders at all.
 *
 * <h2>The bug this exists to prevent</h2>
 *
 * Vanilla's {@code MILK_BUCKET} carries {@code craftRemainder(BUCKET)}, so an ordinary recipe using
 * one gives the empty bucket straight back. That is correct when the product doesn't keep the
 * container — a cake keeps none of its three buckets — but it is <b>a duplication bug</b> when the
 * product <em>is</em> a bucket:
 *
 * <pre>
 *   1 milk bucket in  ->  bhang bucket + 1 empty bucket returned at craft
 *   drink the bhang   ->  1 more empty bucket
 *   ================================================================
 *   one bucket in, two buckets out. Free iron, repeatable.
 * </pre>
 *
 * Overriding {@link #getRemainder} to return nothing makes the bucket carry through: the one you
 * poured the milk from is the one the bhang is in, and it comes back exactly once, when you drink it.
 *
 * <p>Kept generic rather than named after bhang because any future "ingredient's container becomes
 * the product's container" recipe wants precisely this and nothing else.
 *
 * <h2>Why a real recipe subclass rather than a special recipe</h2>
 *
 * The mod's other bespoke recipe, {@link PackingRecipe}, is a {@code CustomRecipe}, which
 * the recipe book and every recipe viewer ignore because it has no fixed ingredient list. This one
 * has perfectly ordinary fixed ingredients — it differs from a shapeless recipe in exactly one
 * method — so extending {@link ShapelessRecipe} keeps it visible in the recipe book and in JEI/EMI
 * for free. Do not "simplify" it into a special recipe; that would trade a visible recipe for an
 * invisible one to save about twenty lines.
 */
public class ContainerCarriedRecipe extends ShapelessRecipe {

    /** Vanilla's shapeless recipe in every field; its serializer reads the file. */
    public ContainerCarriedRecipe(ShapelessRecipe base) {
        super(base.getId(), base.getGroup(), base.getResultItem(), base.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        return NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CONTAINER_CARRIED;
    }

    public static class Serializer extends ModRecipeSerializer<ContainerCarriedRecipe> {
        @Override
        public ContainerCarriedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ContainerCarriedRecipe(RecipeJson.shapeless(id, json));
        }

        @Override
        public ContainerCarriedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new ContainerCarriedRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ContainerCarriedRecipe recipe) {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buf, recipe);
        }
    }
}
