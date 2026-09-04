package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

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
        implements Recipe<SingleStackRecipeInput> {

    @Override
    public boolean matches(SingleStackRecipeInput input, World world) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return result.copy();
    }

    /** One slot, one ingredient — the shape a recipe viewer reads off this. */
    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forSingleSlot(ingredient);
    }

    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }

    /**
     * Never shown, because {@link #isIgnoredInRecipeBook()} is true and {@code getDisplays()} is
     * empty — but {@code Recipe} demands one, so this is the harmless answer.
     */
    @Override
    public RecipeBookCategory getRecipeBookCategory() {
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
                ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(DecarboxylatingRecipe::result)
        ).apply(instance, DecarboxylatingRecipe::new));

        private static final PacketCodec<RegistryByteBuf, DecarboxylatingRecipe> PACKET_CODEC = PacketCodec.tuple(
                Ingredient.PACKET_CODEC, DecarboxylatingRecipe::ingredient,
                ItemStack.PACKET_CODEC, DecarboxylatingRecipe::result,
                DecarboxylatingRecipe::new);

        @Override
        public MapCodec<DecarboxylatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, DecarboxylatingRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
