package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
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
    private final ItemStack result;
    private final int offset;

    public InfusedShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern raw,
                               ItemStack result, int offset) {
        super(group, category, raw, result);
        this.raw = raw;
        this.result = result;
        this.offset = offset;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        return Infusion.transfer(input, super.assemble(input, lookup), offset);
    }

    // As InfusedShapelessRecipe: ShapedRecipe's declaration is invariant, and the cast is sound.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.INFUSED_SHAPED;
    }

    public static class Serializer implements RecipeSerializer<InfusedShapedRecipe> {
        private static final MapCodec<InfusedShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::group),
                CraftingBookCategory.CODEC.fieldOf("category")
                        .orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.raw),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.INT.optionalFieldOf("offset", 0).forGetter(recipe -> recipe.offset)
        ).apply(instance, InfusedShapedRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, InfusedShapedRecipe> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapedRecipe::group,
                CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
                ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.raw,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                ByteBufCodecs.VAR_INT, recipe -> recipe.offset,
                InfusedShapedRecipe::new);

        @Override
        public MapCodec<InfusedShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InfusedShapedRecipe> streamCodec() {
            return PACKET_CODEC;
        }
    }
}
