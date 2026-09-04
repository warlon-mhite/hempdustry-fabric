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
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.Optional;

/**
 * What the Infuser turns into what: the three things it accepts, and the one thing it yields.
 *
 * <p>It is <b>one recipe describing a whole machine</b>, not one recipe per craft, because the
 * machine's two axes cannot be written down as recipes. Strength is how much hemp dissolved in and
 * Quality is a score over the simmer and the washed ratio — both live in the block entity and always
 * will. What was hardcoded and did not need to be is <em>which items</em> play each part, and that is
 * everything here: a pack can point the tub at another mod's milk substitute, accept another mod's
 * decarboxylated hemp, or change what a batch produces, and a recipe viewer can finally show the
 * conversion at all.
 *
 * <p>The two hemp ingredients are separate because the machine treats them differently — washed hemp
 * is what makes {@code Clean} and {@code Perfect} reachable, and absorption spends unwashed first. A
 * single ingredient with a flag would not survive that.
 *
 * <p><b>Removing every infusing recipe disables the machine</b> rather than crashing it: with no
 * recipe the tub accepts nothing and produces nothing, which is a legitimate thing for a pack to want
 * and the only sane reading of "there is no conversion".
 */
public record InfusingRecipe(Ingredient container, Ingredient hemp, Ingredient washedHemp, ItemStack result)
        implements Recipe<SingleStackRecipeInput> {

    /**
     * The conversion this world is running.
     *
     * <p>ponytail: first entry wins if a pack ships more than one — the mod ships exactly one, and
     * per-recipe results would mean persisting the chosen recipe in the block entity's NBT, since the
     * milk that selected it is spent long before the batch is collected. If a second one is ever
     * genuinely wanted, that is the work.
     */
    public static InfusingRecipe of(World world) {
        return ModRecipes.allOfType(world, ModRecipes.INFUSING_TYPE)
                .stream().findFirst().map(RecipeEntry::value).orElse(null);
    }

    @Override
    public boolean matches(SingleStackRecipeInput input, World world) {
        ItemStack stack = input.item();
        return container.test(stack) || hemp.test(stack) || washedHemp.test(stack);
    }

    @Override
    public ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return result.copy();
    }

    /** The three things the tub accepts, in the order the screen lays them out. */
    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forMultipleSlots(
                List.of(Optional.of(container), Optional.of(hemp), Optional.of(washedHemp)));
    }

    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }

    /** Never shown — see {@link DecarboxylatingRecipe#getRecipeBookCategory()}. */
    @Override
    public RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<? extends InfusingRecipe> getSerializer() {
        return ModRecipes.INFUSING;
    }

    @Override
    public RecipeType<? extends InfusingRecipe> getType() {
        return ModRecipes.INFUSING_TYPE;
    }

    /**
     * A representative stack for an ingredient, used when the machine has to hand items <em>back</em>
     * — spilling a broken tub's banked hemp. The batch counts items, not stacks, so once absorbed
     * there is nothing left recording which of an ingredient's matches went in; the first match is
     * the only answer available and is the right one for every single-item ingredient, which both of
     * the shipped ones are.
     */
    public static ItemStack representative(Ingredient ingredient) {
        return ingredient.getMatchingItems().findFirst()
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    public static class Serializer implements RecipeSerializer<InfusingRecipe> {
        private static final MapCodec<InfusingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("container").forGetter(InfusingRecipe::container),
                Ingredient.CODEC.fieldOf("hemp").forGetter(InfusingRecipe::hemp),
                Ingredient.CODEC.fieldOf("washed_hemp").forGetter(InfusingRecipe::washedHemp),
                ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(InfusingRecipe::result)
        ).apply(instance, InfusingRecipe::new));

        private static final PacketCodec<RegistryByteBuf, InfusingRecipe> PACKET_CODEC = PacketCodec.tuple(
                Ingredient.PACKET_CODEC, InfusingRecipe::container,
                Ingredient.PACKET_CODEC, InfusingRecipe::hemp,
                Ingredient.PACKET_CODEC, InfusingRecipe::washedHemp,
                ItemStack.PACKET_CODEC, InfusingRecipe::result,
                InfusingRecipe::new);

        @Override
        public MapCodec<InfusingRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, InfusingRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
