package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
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
 * The shapeless twin of {@link InfusedShapedRecipe} — carries cannabutter's potency and quality onto
 * its output, and is otherwise an ordinary shapeless recipe so it stays recipe-book and JEI visible.
 */
public class InfusedShapelessRecipe extends ShapelessRecipe {
    private final ItemStack result;
    private final List<Ingredient> ingredients;
    private final int offset;

    public InfusedShapelessRecipe(String group, CraftingBookCategory category, ItemStack result,
                                  List<Ingredient> ingredients, int offset) {
        super(group, category, result, toDefaultedList(ingredients));
        this.result = result;
        this.ingredients = ingredients;
        this.offset = offset;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        return Infusion.transfer(input, super.assemble(input, lookup), offset);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSED_SHAPELESS;
    }

    private static NonNullList<Ingredient> toDefaultedList(List<Ingredient> ingredients) {
        NonNullList<Ingredient> list = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) {
            list.set(i, ingredients.get(i));
        }
        return list;
    }

    public static class Serializer implements RecipeSerializer<InfusedShapelessRecipe> {
        private static final MapCodec<InfusedShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category")
                        .orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
                Codec.INT.optionalFieldOf("offset", 0).forGetter(recipe -> recipe.offset)
        ).apply(instance, InfusedShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, InfusedShapelessRecipe> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapelessRecipe::getGroup,
                CraftingBookCategory.STREAM_CODEC, ShapelessRecipe::category,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.ingredients,
                ByteBufCodecs.VAR_INT, recipe -> recipe.offset,
                InfusedShapelessRecipe::new);

        @Override
        public MapCodec<InfusedShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InfusedShapelessRecipe> streamCodec() {
            return PACKET_CODEC;
        }
    }
}
