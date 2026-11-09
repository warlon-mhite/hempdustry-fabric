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
import net.minecraft.world.level.Level;

/**
 * A shaped recipe over stamped hemp matter: it matches only when that matter shares one chemotype,
 * and its result carries it ({@link Stamping}). Nine CBD leaves make a CBD Block of Hemp Leaves, and a
 * spliff of CBD schwag smokes CBD. The JSON is vanilla's shaped recipe, field for field.
 */
public class StampedShapedRecipe extends ShapedRecipe {
    private final ShapedRecipePattern raw;
    private final ItemStack result;

    public StampedShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern raw, ItemStack result) {
        super(group, category, raw, result);
        this.raw = raw;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && Stamping.agrees(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        return Stamping.carry(input, super.assemble(input, lookup));
    }

    // As InfusedShapedRecipe: ShapedRecipe's declaration is invariant, and the cast is sound.
    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.STAMPED_SHAPED;
    }

    public static class Serializer implements RecipeSerializer<StampedShapedRecipe> {
        private static final MapCodec<StampedShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::group),
                CraftingBookCategory.CODEC.fieldOf("category")
                        .orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.raw),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, StampedShapedRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, StampedShapedRecipe> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapedRecipe::group,
                CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
                ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.raw,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                StampedShapedRecipe::new);

        @Override
        public MapCodec<StampedShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StampedShapedRecipe> streamCodec() {
            return PACKET_CODEC;
        }
    }
}
