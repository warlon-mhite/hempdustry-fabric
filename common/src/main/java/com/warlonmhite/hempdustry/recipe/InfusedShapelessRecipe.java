package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
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
 * The shapeless twin of {@link InfusedShapedRecipe} — carries cannabutter's potency and quality onto
 * its output, and is otherwise an ordinary shapeless recipe so it stays recipe-book and JEI visible.
 */
public class InfusedShapelessRecipe extends ShapelessRecipe {
    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;
    private final int offset;

    public InfusedShapelessRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                  ItemStackTemplate result, List<Ingredient> ingredients, int offset) {
        super(commonInfo, bookInfo, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
        this.offset = offset;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return Infusion.transfer(input, super.assemble(input), offset);
    }

    // ShapelessRecipe declares RecipeSerializer<ShapelessRecipe> — invariant, so an override cannot
    // narrow it. The cast is sound: this serializer only ever builds a InfusedShapelessRecipe.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) ModRecipes.INFUSED_SHAPELESS;
    }

    /** The recipe's JSON; {@link ModRecipes} makes its serializer of this and {@link #STREAM_CODEC}. */
    public static final MapCodec<InfusedShapelessRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
            Codec.INT.optionalFieldOf("offset", 0).forGetter(recipe -> recipe.offset)
    ).apply(instance, InfusedShapelessRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusedShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.ingredients,
            ByteBufCodecs.VAR_INT, recipe -> recipe.offset,
            InfusedShapelessRecipe::new);
}
