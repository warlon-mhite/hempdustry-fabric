package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * A shaped recipe over stamped hemp matter: it matches only when that matter shares one chemotype,
 * and its result carries it ({@link Stamping}). Nine CBD leaves make a CBD Block of Hemp Leaves, and a
 * spliff of CBD schwag smokes CBD. The JSON is vanilla's shaped recipe, field for field.
 */
public class StampedShapedRecipe extends ShapedRecipe {
    private final ShapedRecipePattern raw;
    private final ItemStackTemplate result;

    public StampedShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                               ShapedRecipePattern raw, ItemStackTemplate result) {
        super(commonInfo, bookInfo, raw, result);
        this.raw = raw;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && Stamping.agrees(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return Stamping.carry(input, super.assemble(input));
    }

    // As InfusedShapedRecipe: ShapedRecipe's declaration is invariant, and the cast is sound.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.STAMPED_SHAPED;
    }

    public static final MapCodec<StampedShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.raw),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
    ).apply(instance, StampedShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StampedShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.raw,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
            StampedShapedRecipe::new);
}
