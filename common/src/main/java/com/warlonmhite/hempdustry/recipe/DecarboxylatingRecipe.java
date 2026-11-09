package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * One tray-load in the Decarboxylator: an ingredient in, a stack of decarboxylated hemp out.
 *
 * <p><b>The machine used to compare item identity in Java</b> — indica buds, sativa buds, hemp leaf,
 * and nothing else could ever be added. That was the one place the mod's own extensibility stopped
 * dead: a third-party strain could get all the way to the oven through the strain registry and the
 * tags, and then had nowhere to go. A recipe type is the fix, and it buys three more things on the
 * way past — KubeJS and CraftTweaker can rebalance the chain, a recipe viewer can show it, and a
 * datapack can change a yield.
 *
 * <p><b>No cook time here, deliberately.</b> The oven runs one clock for all three trays and syncs it
 * to the screen through a single property; a per-recipe time would need three more properties and
 * would let one tray finish while the arrow beside it is drawn from another recipe's scale. The speed
 * knob is {@code world.machineSpeedMultiplier} in the config, which moves all three together.
 *
 * <p>Not in the recipe book: the book only knows the crafting types, and a machine recipe there would
 * be an entry the player could not click. Recipe viewers read the type directly.
 *
 * <h2>A bud's count is its strain's</h2>
 *
 * A strain's {@code potency.base} is what one of its mid buds is worth, to the oven and to the bowl
 * alike, so the two never disagree. A bud's recipe says so with {@code "count_from_strain": true}
 * ({@link #countFromStrain}): the oven then takes the strain's base, and the result's count is only
 * what a viewer or another mod reading the file sees. A recipe that leaves the field out keeps its
 * own count, which then moves the oven alone: a pack retuning edibles changes no bowl, and a 2.1
 * datapack recipe with a count works as it did. A separate field because a result's count cannot be
 * left out: a result stack reads a missing one as 1. A recipe whose count disagrees with
 * its bud's strain is named in the log once the server is up ({@link #reportOverrides}).
 */
public record DecarboxylatingRecipe(Ingredient ingredient, ItemStack result, boolean countFromStrain)
        implements Recipe<SingleRecipeInput> {

    /** A recipe that keeps its own count: a leaf's, schwag's, or anything 2.1 wrote. */
    public DecarboxylatingRecipe(Ingredient ingredient, ItemStack result) {
        this(ingredient, result, false);
    }

    /**
     * What one mid piece of {@code input} bakes into: its strain's base when this recipe takes it and
     * the piece is a plant's bud, otherwise the result's count. A band then moves it.
     */
    public int midCount(ItemStack input, HolderLookup.Provider registries) {
        if (countFromStrain) {
            Optional<Integer> base = strainBase(input.getItem(), registries);
            if (base.isPresent()) {
                return base.get();
            }
        }
        return result.getCount();
    }

    private static Optional<Integer> strainBase(Item item, HolderLookup.Provider registries) {
        return Strain.fromBuds(registries, item)
                .filter(strain -> strain.value().wildFlower().isPresent())
                .map(strain -> strain.value().potency().base());
    }

    /**
     * Names in the log every recipe that writes its own count for a plant strain's bud when that count
     * is not the strain's base: the oven then pays it, while the bowl still weighs the base. Not a
     * mistake on its face (a pack may want exactly that), so a plain line, not a warning.
     */
    public static void reportOverrides(MinecraftServer server) {
        HolderLookup.Provider registries = server.registryAccess();
        for (RecipeHolder<DecarboxylatingRecipe> holder : ModRecipes.allOfType(server.overworld(), ModRecipes.DECARBOXYLATING_TYPE)) {
            DecarboxylatingRecipe recipe = holder.value();
            if (recipe.countFromStrain()) {
                continue;
            }
            recipe.ingredient().items().forEach(item -> strainBase(item.value(), registries)
                    .filter(base -> base != recipe.result().getCount())
                    .ifPresent(base -> Hempdustry.LOGGER.info(
                            "Decarboxylating recipe {} bakes {} into {}, not its strain's base {}: the oven pays the recipe, the bowl the base",
                            holder.id().identifier(), item.getRegisteredName(), recipe.result().getCount(), base)));
        }
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level world) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider lookup) {
        return result.copy();
    }

    /** One slot, one ingredient — the shape a recipe viewer reads off this. */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredient);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * Never shown, because {@link #isSpecial()} is true and {@code display()} is
     * empty — but {@code Recipe} demands one, so this is the harmless answer.
     */
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<? extends DecarboxylatingRecipe> getSerializer() {
        return ModRecipes.DECARBOXYLATING;
    }

    @Override
    public RecipeType<? extends DecarboxylatingRecipe> getType() {
        return ModRecipes.DECARBOXYLATING_TYPE;
    }

    public static class Serializer implements RecipeSerializer<DecarboxylatingRecipe> {
        private static final MapCodec<DecarboxylatingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(DecarboxylatingRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(DecarboxylatingRecipe::result),
                Codec.BOOL.optionalFieldOf("count_from_strain", false).forGetter(DecarboxylatingRecipe::countFromStrain)
        ).apply(instance, DecarboxylatingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, DecarboxylatingRecipe> PACKET_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, DecarboxylatingRecipe::ingredient,
                ItemStack.STREAM_CODEC, DecarboxylatingRecipe::result,
                ByteBufCodecs.BOOL, DecarboxylatingRecipe::countFromStrain,
                DecarboxylatingRecipe::new);

        @Override
        public MapCodec<DecarboxylatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DecarboxylatingRecipe> streamCodec() {
            return PACKET_CODEC;
        }
    }
}
