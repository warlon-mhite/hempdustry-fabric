package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that carries cannabutter's potency and quality onto its output — see
 * {@link Infusion}. Ordinary in every other respect, and deliberately a {@link ShapedRecipe} subclass
 * rather than a special recipe so it stays visible in the recipe book and in JEI/EMI.
 *
 * <p>{@code offset} is the edible's own step on the potency ladder: {@code +1} concentrates,
 * {@code 0} is neutral, {@code -1} spreads the butter thin.
 */
public class InfusedShapedRecipe extends ShapedRecipe {
    private final ShapedRecipePattern raw;
    private final ItemStackTemplate result;
    private final int offset;

    public InfusedShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                               ShapedRecipePattern raw, ItemStackTemplate result, int offset) {
        super(commonInfo, bookInfo, raw, result);
        this.raw = raw;
        this.result = result;
        this.offset = offset;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return Infusion.transfer(input, super.assemble(input), offset);
    }

    // As InfusedShapelessRecipe: ShapedRecipe's declaration is invariant, and the cast is sound.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.INFUSED_SHAPED;
    }

    /** The recipe's JSON; {@link ModRecipes} makes its serializer of this and {@link #STREAM_CODEC}. */
    public static final MapCodec<InfusedShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.raw),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Codec.INT.optionalFieldOf("offset", 0).forGetter(recipe -> recipe.offset)
    ).apply(instance, InfusedShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusedShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.raw,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
            ByteBufCodecs.VAR_INT, recipe -> recipe.offset,
            InfusedShapedRecipe::new);
}
