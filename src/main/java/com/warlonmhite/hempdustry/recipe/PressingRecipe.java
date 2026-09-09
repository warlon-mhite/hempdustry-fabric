package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

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
public record PressingRecipe(Ingredient ingredient, ItemStack result)
        implements Recipe<SingleStackRecipeInput> {

    @Override
    public boolean matches(SingleStackRecipeInput input, World world) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return result.copy();
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forSingleSlot(ingredient);
    }

    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }

    /** Never shown — {@code Recipe} demands one, so this is the harmless answer. */
    @Override
    public RecipeBookCategory getRecipeBookCategory() {
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

    public static class Serializer implements RecipeSerializer<PressingRecipe> {
        private static final MapCodec<PressingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(PressingRecipe::ingredient),
                ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(PressingRecipe::result)
        ).apply(instance, PressingRecipe::new));

        private static final PacketCodec<RegistryByteBuf, PressingRecipe> PACKET_CODEC = PacketCodec.tuple(
                Ingredient.PACKET_CODEC, PressingRecipe::ingredient,
                ItemStack.PACKET_CODEC, PressingRecipe::result,
                PressingRecipe::new);

        @Override
        public MapCodec<PressingRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, PressingRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
