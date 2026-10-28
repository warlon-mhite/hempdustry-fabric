package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
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
    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;

    public ContainerCarriedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                  ItemStackTemplate result, List<Ingredient> ingredients) {
        super(commonInfo, bookInfo, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
    }

    /** No remainders: every ingredient's container is considered part of the product. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    // ShapelessRecipe declares RecipeSerializer<ShapelessRecipe> — invariant, so an override cannot
    // narrow it. The cast is sound: this serializer only ever builds a ContainerCarriedRecipe.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) ModRecipes.CONTAINER_CARRIED;
    }

    /** The recipe's JSON; {@link ModRecipes} makes its serializer of this and {@link #STREAM_CODEC}. */
    public static final MapCodec<ContainerCarriedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients)
    ).apply(instance, ContainerCarriedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerCarriedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.ingredients,
            ContainerCarriedRecipe::new);
}
