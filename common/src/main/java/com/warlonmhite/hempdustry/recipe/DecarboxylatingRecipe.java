package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
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
public record DecarboxylatingRecipe(Ingredient ingredient, ItemStack result)
        implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level world) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider lookup) {
        return result.copy();
    }

    /** One slot, one ingredient — the shape a recipe viewer reads off this. */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredient);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * Never shown, because {@link #isSpecial()} is true and {@code display()} is
     * empty — but {@code Recipe} demands one, so this is the harmless answer.
     */
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<? extends DecarboxylatingRecipe> getSerializer() {
        return ModRecipes.DECARBOXYLATING;
    }

    @Override
    public RecipeType<? extends DecarboxylatingRecipe> getType() {
        return ModRecipes.DECARBOXYLATING_TYPE;
    }

    public static class Serializer implements RecipeSerializer<DecarboxylatingRecipe> {
        private static final MapCodec<DecarboxylatingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(DecarboxylatingRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(DecarboxylatingRecipe::result)
        ).apply(instance, DecarboxylatingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, DecarboxylatingRecipe> PACKET_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, DecarboxylatingRecipe::ingredient,
                ItemStack.STREAM_CODEC, DecarboxylatingRecipe::result,
                DecarboxylatingRecipe::new);

        @Override
        public MapCodec<DecarboxylatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DecarboxylatingRecipe> streamCodec() {
            return PACKET_CODEC;
        }
    }
}
