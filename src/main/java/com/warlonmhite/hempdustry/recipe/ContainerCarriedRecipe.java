package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

/**
 * A shapeless recipe whose ingredients' containers <em>travel into the result</em> instead of being
 * handed back — i.e. one that returns no recipe remainders at all.
 *
 * <h2>The bug this exists to prevent</h2>
 *
 * Vanilla's {@code MILK_BUCKET} carries {@code recipeRemainder(BUCKET)}, so an ordinary recipe using
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
 * The mod's other bespoke recipe, {@link PackingRecipe}, is a {@code SpecialCraftingRecipe}, which
 * the recipe book and every recipe viewer ignore because it has no fixed ingredient list. This one
 * has perfectly ordinary fixed ingredients — it differs from a shapeless recipe in exactly one
 * method — so extending {@link ShapelessRecipe} keeps it visible in the recipe book and in JEI/EMI
 * for free. Do not "simplify" it into a special recipe; that would trade a visible recipe for an
 * invisible one to save about twenty lines.
 */
public class ContainerCarriedRecipe extends ShapelessRecipe {
    private final ItemStack result;
    private final List<Ingredient> ingredients;

    public ContainerCarriedRecipe(String group, CraftingRecipeCategory category, ItemStack result,
                                  List<Ingredient> ingredients) {
        super(group, category, result, toDefaultedList(ingredients));
        this.result = result;
        this.ingredients = ingredients;
    }

    /** No remainders: every ingredient's container is considered part of the product. */
    @Override
    public DefaultedList<ItemStack> getRemainder(CraftingRecipeInput input) {
        return DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CONTAINER_CARRIED;
    }

    private static DefaultedList<Ingredient> toDefaultedList(List<Ingredient> ingredients) {
        DefaultedList<Ingredient> list = DefaultedList.ofSize(ingredients.size(), Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) {
            list.set(i, ingredients.get(i));
        }
        return list;
    }

    public static class Serializer implements RecipeSerializer<ContainerCarriedRecipe> {
        private static final MapCodec<ContainerCarriedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                CraftingRecipeCategory.CODEC.fieldOf("category")
                        .orElse(CraftingRecipeCategory.MISC).forGetter(ShapelessRecipe::getCategory),
                ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Ingredient.DISALLOW_EMPTY_CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients)
        ).apply(instance, ContainerCarriedRecipe::new));

        private static final PacketCodec<RegistryByteBuf, ContainerCarriedRecipe> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ShapelessRecipe::getGroup,
                CraftingRecipeCategory.PACKET_CODEC, ShapelessRecipe::getCategory,
                ItemStack.PACKET_CODEC, recipe -> recipe.result,
                Ingredient.PACKET_CODEC.collect(PacketCodecs.toList()), recipe -> recipe.ingredients,
                ContainerCarriedRecipe::new);

        @Override
        public MapCodec<ContainerCarriedRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, ContainerCarriedRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
