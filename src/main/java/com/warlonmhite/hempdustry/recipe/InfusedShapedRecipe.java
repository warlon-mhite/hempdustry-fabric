package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;

/**
 * A shaped recipe that carries cannabutter's potency and quality onto its output — see
 * {@link Infusion}. Ordinary in every other respect, and deliberately a {@link ShapedRecipe} subclass
 * rather than a special recipe so it stays visible in the recipe book and in JEI/EMI.
 *
 * <p>{@code offset} is the edible's own step on the potency ladder: {@code +1} concentrates,
 * {@code 0} is neutral, {@code -1} spreads the butter thin.
 */
public class InfusedShapedRecipe extends ShapedRecipe {
    private final RawShapedRecipe raw;
    private final ItemStack result;
    private final int offset;

    public InfusedShapedRecipe(String group, CraftingRecipeCategory category, RawShapedRecipe raw,
                               ItemStack result, int offset) {
        super(group, category, raw, result);
        this.raw = raw;
        this.result = result;
        this.offset = offset;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return Infusion.transfer(input, super.craft(input, lookup), offset);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSED_SHAPED;
    }

    public static class Serializer implements RecipeSerializer<InfusedShapedRecipe> {
        private static final MapCodec<InfusedShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingRecipeCategory.CODEC.fieldOf("category")
                        .orElse(CraftingRecipeCategory.MISC).forGetter(ShapedRecipe::getCategory),
                RawShapedRecipe.CODEC.forGetter(recipe -> recipe.raw),
                ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.INT.optionalFieldOf("offset", 0).forGetter(recipe -> recipe.offset)
        ).apply(instance, InfusedShapedRecipe::new));

        private static final PacketCodec<RegistryByteBuf, InfusedShapedRecipe> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ShapedRecipe::getGroup,
                CraftingRecipeCategory.PACKET_CODEC, ShapedRecipe::getCategory,
                RawShapedRecipe.PACKET_CODEC, recipe -> recipe.raw,
                ItemStack.PACKET_CODEC, recipe -> recipe.result,
                PacketCodecs.VAR_INT, recipe -> recipe.offset,
                InfusedShapedRecipe::new);

        @Override
        public MapCodec<InfusedShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, InfusedShapedRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
