package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.block.ModBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

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
public record InfusingRecipe(ResourceLocation id, Ingredient container, Ingredient hemp, Ingredient washedHemp,
                             Optional<Ingredient> scorchedHemp, ItemStack result)
        implements Recipe<Container> {

    /**
     * The conversion this world is running.
     *
     * <p>ponytail: first entry wins if a pack ships more than one — the mod ships exactly one, and
     * per-recipe results would mean persisting the chosen recipe in the block entity's NBT, since the
     * milk that selected it is spent long before the batch is collected. If a second one is ever
     * genuinely wanted, that is the work.
     */
    public static InfusingRecipe of(Level world) {
        List<InfusingRecipe> all = world.getRecipeManager().getAllRecipesFor(ModRecipes.INFUSING_TYPE);
        return all.isEmpty() ? null : all.get(0);
    }

    @Override
    public boolean matches(Container input, Level world) {
        ItemStack stack = input.getItem(0);
        return container.test(stack) || hemp.test(stack) || washedHemp.test(stack) || isScorched(stack);
    }

    /** Whether {@code stack} plays the scorched part — never, for a recipe that does not name one. */
    public boolean isScorched(ItemStack stack) {
        return scorchedHemp.isPresent() && scorchedHemp.get().test(stack);
    }

    @Override
    public ItemStack assemble(Container input) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.withSize(scorchedHemp.isPresent() ? 4 : 3, Ingredient.EMPTY);
        list.set(0, container);
        list.set(1, hemp);
        list.set(2, washedHemp);
        scorchedHemp.ifPresent(scorched -> list.set(3, scorched));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.INFUSER);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSING;
    }

    @Override
    public RecipeType<?> getType() {
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
        ItemStack[] matches = ingredient.getItems();
        return matches.length == 0 ? ItemStack.EMPTY : matches[0];
    }

    public static class Serializer extends ModRecipeSerializer<InfusingRecipe> {
        @Override
        public InfusingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new InfusingRecipe(id,
                    RecipeJson.ingredient(json, "container"),
                    RecipeJson.ingredient(json, "hemp"),
                    RecipeJson.ingredient(json, "washed_hemp"),
                    // Optional: see the class notes. Every infusing.json written before it existed
                    // still parses, and means "this tub takes no scorched hemp".
                    json.has("scorched_hemp")
                            ? Optional.of(RecipeJson.ingredient(json, "scorched_hemp"))
                            : Optional.empty(),
                    RecipeJson.result(json, "result"));
        }

        @Override
        public InfusingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new InfusingRecipe(id,
                    Ingredient.fromNetwork(buf),
                    Ingredient.fromNetwork(buf),
                    Ingredient.fromNetwork(buf),
                    buf.readOptional(Ingredient::fromNetwork),
                    buf.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, InfusingRecipe recipe) {
            recipe.container().toNetwork(buf);
            recipe.hemp().toNetwork(buf);
            recipe.washedHemp().toNetwork(buf);
            buf.writeOptional(recipe.scorchedHemp(), (out, ingredient) -> ingredient.toNetwork(out));
            buf.writeItem(recipe.result());
        }
    }
}
