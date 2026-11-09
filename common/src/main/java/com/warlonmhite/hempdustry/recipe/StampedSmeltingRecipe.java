package com.warlonmhite.hempdustry.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/**
 * Vanilla smelting whose result keeps the chemotype of what burned: a CBD leaf or bud chars into CBD
 * scorched hemp. A bud is its strain's chemotype, anything else its stamp ({@link Stamping#matter}).
 * Still a {@code minecraft:smelting} recipe to every furnace, only with its own serializer.
 */
public class StampedSmeltingRecipe extends SmeltingRecipe {

    public StampedSmeltingRecipe(String group, CookingBookCategory category, Ingredient ingredient,
                                 ItemStack result, float experience, int cookingTime) {
        super(group, category, ingredient, result, experience, cookingTime);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider lookup) {
        return Stamping.apply(super.assemble(input, lookup), Stamping.matter(input.item(), lookup));
    }

    // SmeltingRecipe declares RecipeSerializer<SmeltingRecipe> — invariant; this serializer only
    // ever builds a StampedSmeltingRecipe, so the cast is sound.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<SmeltingRecipe> getSerializer() {
        return (RecipeSerializer<SmeltingRecipe>) (RecipeSerializer<?>) ModRecipes.STAMPED_SMELTING;
    }

    /** Vanilla's smelting serializer, building this class: 200 ticks when the JSON names none. */
    public static RecipeSerializer<StampedSmeltingRecipe> serializer() {
        return new AbstractCookingRecipe.Serializer<>(StampedSmeltingRecipe::new, 200);
    }
}
