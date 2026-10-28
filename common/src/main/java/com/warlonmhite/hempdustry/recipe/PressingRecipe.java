package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
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
public record PressingRecipe(Ingredient ingredient, ItemStackTemplate result)
        implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level world) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result.create();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredient);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    /** Never shown — {@code Recipe} demands one, so this is the harmless answer. */
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<? extends PressingRecipe> getSerializer() {
        return ModRecipes.PRESSING;
    }

    @Override
    public RecipeType<? extends PressingRecipe> getType() {
        return ModRecipes.PRESSING_TYPE;
    }

    /** The recipe's JSON; {@link ModRecipes} makes its serializer of this and {@link #STREAM_CODEC}. */
    public static final MapCodec<PressingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(PressingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(PressingRecipe::result)
    ).apply(instance, PressingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, PressingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, PressingRecipe::result,
            PressingRecipe::new);
}
