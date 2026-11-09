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
import net.minecraft.world.level.Level;

/**
 * The shapeless twin of {@link StampedShapedRecipe}: a block of nine unpacked into stamped pieces, a
 * moon rock coated in stamped hash. One chemotype to a craft, and the result carries it.
 */
public class StampedShapelessRecipe extends ShapelessRecipe {
    private final ItemStack result;
    private final List<Ingredient> ingredients;

    public StampedShapelessRecipe(String group, CraftingBookCategory category, ItemStack result,
                                  List<Ingredient> ingredients) {
        super(group, category, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && Stamping.agrees(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        return Stamping.carry(input, super.assemble(input, lookup), lookup);
    }

    // As InfusedShapelessRecipe: ShapelessRecipe's declaration is invariant, and the cast is sound.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) ModRecipes.STAMPED_SHAPELESS;
    }

    public static class Serializer implements RecipeSerializer<StampedShapelessRecipe> {
        private static final MapCodec<StampedShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::group),
                CraftingBookCategory.CODEC.fieldOf("category")
                        .orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients)
        ).apply(instance, StampedShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, StampedShapelessRecipe> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapelessRecipe::group,
                CraftingBookCategory.STREAM_CODEC, ShapelessRecipe::category,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.ingredients,
                StampedShapelessRecipe::new);

        @Override
        public MapCodec<StampedShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StampedShapelessRecipe> streamCodec() {
            return PACKET_CODEC;
        }
    }
}
