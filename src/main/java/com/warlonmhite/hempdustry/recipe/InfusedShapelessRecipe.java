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
 * The shapeless twin of {@link InfusedShapedRecipe} — carries cannabutter's potency and quality onto
 * its output, and is otherwise an ordinary shapeless recipe so it stays recipe-book and JEI visible.
 */
public class InfusedShapelessRecipe extends ShapelessRecipe {
    private final ItemStack result;
    private final List<Ingredient> ingredients;
    private final int offset;

    public InfusedShapelessRecipe(String group, CraftingRecipeCategory category, ItemStack result,
                                  List<Ingredient> ingredients, int offset) {
        super(group, category, result, toDefaultedList(ingredients));
        this.result = result;
        this.ingredients = ingredients;
        this.offset = offset;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return Infusion.transfer(input, super.craft(input, lookup), offset);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSED_SHAPELESS;
    }

    private static DefaultedList<Ingredient> toDefaultedList(List<Ingredient> ingredients) {
        DefaultedList<Ingredient> list = DefaultedList.ofSize(ingredients.size(), Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) {
            list.set(i, ingredients.get(i));
        }
        return list;
    }

    public static class Serializer implements RecipeSerializer<InfusedShapelessRecipe> {
        private static final MapCodec<InfusedShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                CraftingRecipeCategory.CODEC.fieldOf("category")
                        .orElse(CraftingRecipeCategory.MISC).forGetter(ShapelessRecipe::getCategory),
                ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Ingredient.DISALLOW_EMPTY_CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
                Codec.INT.optionalFieldOf("offset", 0).forGetter(recipe -> recipe.offset)
        ).apply(instance, InfusedShapelessRecipe::new));

        private static final PacketCodec<RegistryByteBuf, InfusedShapelessRecipe> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ShapelessRecipe::getGroup,
                CraftingRecipeCategory.PACKET_CODEC, ShapelessRecipe::getCategory,
                ItemStack.PACKET_CODEC, recipe -> recipe.result,
                Ingredient.PACKET_CODEC.collect(PacketCodecs.toList()), recipe -> recipe.ingredients,
                PacketCodecs.VAR_INT, recipe -> recipe.offset,
                InfusedShapelessRecipe::new);

        @Override
        public MapCodec<InfusedShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, InfusedShapelessRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
