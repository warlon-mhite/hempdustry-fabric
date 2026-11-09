package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/**
 * Vanilla smelting whose result keeps the chemotype of what burned: a CBD leaf or bud chars into CBD
 * scorched hemp. A bud is its strain's chemotype, anything else its stamp ({@link Stamping#matter}).
 * Still a {@code minecraft:smelting} recipe to every furnace, only with its own serializer.
 */
public class StampedSmeltingRecipe extends SmeltingRecipe {

    public StampedSmeltingRecipe(Recipe.CommonInfo commonInfo, AbstractCookingRecipe.CookingBookInfo bookInfo,
                                 Ingredient ingredient, ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, ingredient, result, experience, cookingTime);
    }

    /**
     * The result as the recipe file writes it, before {@link #assemble} stamps it: what a machine of
     * another mod gets when it copies the result rather than assembling it.
     */
    public ItemStackTemplate template() {
        return result();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return Stamping.apply(super.assemble(input), Stamping.matter(input.item()));
    }

    // SmeltingRecipe declares RecipeSerializer<SmeltingRecipe> — invariant; this serializer only
    // ever builds a StampedSmeltingRecipe, so the cast is sound.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<SmeltingRecipe> getSerializer() {
        return (RecipeSerializer<SmeltingRecipe>) (RecipeSerializer<?>) ModRecipes.STAMPED_SMELTING;
    }

    public static final MapCodec<StampedSmeltingRecipe> MAP_CODEC =
            AbstractCookingRecipe.cookingMapCodec(StampedSmeltingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, StampedSmeltingRecipe> STREAM_CODEC =
            AbstractCookingRecipe.cookingStreamCodec(StampedSmeltingRecipe::new);
}
