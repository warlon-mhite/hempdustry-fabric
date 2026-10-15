package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
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
    private final ItemStack result;
    private final List<Ingredient> ingredients;

    public ContainerCarriedRecipe(String group, CraftingBookCategory category, ItemStack result,
                                  List<Ingredient> ingredients) {
        super(group, category, result, toDefaultedList(ingredients));
        this.result = result;
        this.ingredients = ingredients;
    }

    /** No remainders: every ingredient's container is considered part of the product. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CONTAINER_CARRIED;
    }

    private static NonNullList<Ingredient> toDefaultedList(List<Ingredient> ingredients) {
        NonNullList<Ingredient> list = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) {
            list.set(i, ingredients.get(i));
        }
        return list;
    }

    public static class Serializer implements RecipeSerializer<ContainerCarriedRecipe> {
        private static final MapCodec<ContainerCarriedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category")
                        .orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients)
        ).apply(instance, ContainerCarriedRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ContainerCarriedRecipe> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapelessRecipe::getGroup,
                CraftingBookCategory.STREAM_CODEC, ShapelessRecipe::category,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.ingredients,
                ContainerCarriedRecipe::new);

        @Override
        public MapCodec<ContainerCarriedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ContainerCarriedRecipe> streamCodec() {
            return PACKET_CODEC;
        }
    }
}
