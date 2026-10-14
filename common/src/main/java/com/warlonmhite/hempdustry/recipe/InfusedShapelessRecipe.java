package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
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
        super(group, category, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
        this.offset = offset;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        return Infusion.transfer(input, super.assemble(input, lookup), offset);
    }

    // ShapelessRecipe declares RecipeSerializer<ShapelessRecipe> — invariant, so an override cannot
    // narrow it. The cast is sound: this serializer only ever builds a InfusedShapelessRecipe.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) ModRecipes.INFUSED_SHAPELESS;
    }


    public static class Serializer implements RecipeSerializer<InfusedShapelessRecipe> {
        private static final MapCodec<InfusedShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::group),
                CraftingBookCategory.CODEC.fieldOf("category")
                        .orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
                Codec.INT.optionalFieldOf("offset", 0).forGetter(recipe -> recipe.offset)
        ).apply(instance, InfusedShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, InfusedShapelessRecipe> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapelessRecipe::group,
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
