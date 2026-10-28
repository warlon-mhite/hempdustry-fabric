package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import java.util.Optional;

/**
 * What the Infuser turns into what: the three things it accepts, and the one thing it yields.
 *
 * <p>It is <b>one recipe describing a whole machine</b>, not one recipe per craft, because the
 * machine's two axes cannot be written down as recipes. Strength is how much hemp dissolved in and
 * Quality is a score over the simmer and the washed ratio — both live in the block entity and always
 * will. What was hardcoded and did not need to be is <em>which items</em> play each part, and that is
 * everything here: a pack can point the tub at another mod's milk substitute, accept another mod's
 * decarboxylated hemp, or change what a batch produces, and a recipe viewer can finally show the
 * conversion at all.
 *
 * <p>The hemp ingredients are separate because the machine treats them differently — washed hemp
 * is what makes {@code Clean} and {@code Perfect} reachable, scorched hemp counts a quarter and
 * pulls the grade down, and absorption spends the cheapest first. A single ingredient with a flag
 * would not survive that.
 *
 * <p><b>{@code scorched_hemp} is optional, and has to be.</b> It was added after the recipe shipped,
 * so a pack that overrides {@code infusing.json} with the three fields it knew about must keep
 * loading — a required field would take the whole tub down with a parse error. Absent means the tub
 * simply does not take scorched hemp, which is what that pack was written against.
 *
 * <p><b>Removing every infusing recipe disables the machine</b> rather than crashing it: with no
 * recipe the tub accepts nothing and produces nothing, which is a legitimate thing for a pack to want
 * and the only sane reading of "there is no conversion".
 */
public record InfusingRecipe(Ingredient container, Ingredient hemp, Ingredient washedHemp,
                             Optional<Ingredient> scorchedHemp, ItemStackTemplate result)
        implements Recipe<SingleRecipeInput> {

    /**
     * The conversion this world is running.
     *
     * <p>ponytail: first entry wins if a pack ships more than one — the mod ships exactly one, and
     * per-recipe results would mean persisting the chosen recipe in the block entity's NBT, since the
     * milk that selected it is spent long before the batch is collected. If a second one is ever
     * genuinely wanted, that is the work.
     */
    public static InfusingRecipe of(Level world) {
        return ModRecipes.allOfType(world, ModRecipes.INFUSING_TYPE)
                .stream().findFirst().map(RecipeHolder::value).orElse(null);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level world) {
        ItemStack stack = input.item();
        return container.test(stack) || hemp.test(stack) || washedHemp.test(stack) || isScorched(stack);
    }

    /** Whether {@code stack} plays the scorched part — never, for a recipe that does not name one. */
    public boolean isScorched(ItemStack stack) {
        return scorchedHemp.isPresent() && scorchedHemp.get().test(stack);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result.create();
    }

    /** The things the tub accepts, in the order the screen lays them out. */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.createFromOptionals(
                List.of(Optional.of(container), Optional.of(hemp), Optional.of(washedHemp), scorchedHemp));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    /** Never shown — see {@link DecarboxylatingRecipe#recipeBookCategory()}. */
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<? extends InfusingRecipe> getSerializer() {
        return ModRecipes.INFUSING;
    }

    @Override
    public RecipeType<? extends InfusingRecipe> getType() {
        return ModRecipes.INFUSING_TYPE;
    }

    /**
     * A representative stack for an ingredient, used when the machine has to hand items <em>back</em>
     * — spilling a broken tub's banked hemp. The batch counts items, not stacks, so once absorbed
     * there is nothing left recording which of an ingredient's matches went in; the first match is
     * the only answer available and is the right one for every single-item ingredient, which both of
     * the shipped ones are.
     */
    public static ItemStack representative(Ingredient ingredient) {
        return ingredient.items().findFirst()
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** The recipe's JSON; {@link ModRecipes} makes its serializer of this and {@link #STREAM_CODEC}. */
    public static final MapCodec<InfusingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("container").forGetter(InfusingRecipe::container),
            Ingredient.CODEC.fieldOf("hemp").forGetter(InfusingRecipe::hemp),
            Ingredient.CODEC.fieldOf("washed_hemp").forGetter(InfusingRecipe::washedHemp),
            // Optional: see the class notes. Every infusing.json written before it existed
            // still parses, and means "this tub takes no scorched hemp".
            Ingredient.CODEC.optionalFieldOf("scorched_hemp").forGetter(InfusingRecipe::scorchedHemp),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(InfusingRecipe::result)
    ).apply(instance, InfusingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, InfusingRecipe::container,
            Ingredient.CONTENTS_STREAM_CODEC, InfusingRecipe::hemp,
            Ingredient.CONTENTS_STREAM_CODEC, InfusingRecipe::washedHemp,
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, InfusingRecipe::scorchedHemp,
            ItemStackTemplate.STREAM_CODEC, InfusingRecipe::result,
            InfusingRecipe::new);
}
