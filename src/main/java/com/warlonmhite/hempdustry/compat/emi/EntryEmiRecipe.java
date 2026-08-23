package com.warlonmhite.hempdustry.compat.emi;

import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * One {@link ViewerRecipes.Entry} drawn as an EMI page: the inputs in a row, an arrow, the output,
 * and the entry's notes underneath.
 *
 * <p>One class for all three categories rather than one per category, because they differ only in
 * how many slots are on the left — the layout maths is the same and duplicating it three times is
 * three places for it to drift.
 */
public class EntryEmiRecipe implements EmiRecipe {

    private static final int SLOT = 18;
    private static final int ARROW_WIDTH = 24;
    private static final int PADDING = 2;
    /** Room under the slots for the notes. Nine pixels a line is vanilla's own line height. */
    private static final int LINE_HEIGHT = 9;

    private final EmiRecipeCategory category;
    private final ViewerRecipes.Entry entry;
    private final List<EmiIngredient> inputs;
    private final EmiStack output;

    public EntryEmiRecipe(EmiRecipeCategory category, ViewerRecipes.Entry entry) {
        this.category = category;
        this.entry = entry;
        this.inputs = entry.inputs().stream().map(EntryEmiRecipe::ingredient).toList();
        this.output = EmiStack.of(entry.output());
    }

    /**
     * EMI has no {@code Ingredient} overload, so a tag ingredient has to be expanded into the stacks
     * it matches — which is also what makes {@code #hempdustry:milk_buckets} show as "any of these"
     * rather than as one arbitrary bucket.
     */
    private static EmiIngredient ingredient(Ingredient ingredient) {
        return EmiIngredient.of(List.of(ingredient.getMatchingStacks()).stream()
                .map(EmiStack::of)
                .toList());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    /**
     * EMI wants a made-up recipe's path prefixed with {@code /}, so it can tell a display-only row
     * from a real recipe that has gone missing. See {@link ViewerRecipes#synthetic}.
     */
    @Override
    public Identifier getId() {
        return entry.synthetic() ? ViewerRecipes.synthetic(entry.id()) : entry.id();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(output);
    }

    @Override
    public int getDisplayWidth() {
        return inputs.size() * SLOT + ARROW_WIDTH + SLOT + PADDING * 2;
    }

    @Override
    public int getDisplayHeight() {
        return SLOT + PADDING + entry.notes().size() * LINE_HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = PADDING;
        for (EmiIngredient input : inputs) {
            widgets.addSlot(input, x, 0);
            x += SLOT;
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, x + 4, 1);
        x += ARROW_WIDTH;
        widgets.addSlot(output, x, 0).recipeContext(this);

        int y = SLOT + PADDING;
        for (Text note : entry.notes()) {
            widgets.addText(note, 1, y, 0x555555, false);
            y += LINE_HEIGHT;
        }
    }

    /**
     * These are machine conversions, not craftable-in-a-grid recipes, so EMI's recipe tree has
     * nothing to offer for them — it cannot put hemp in a tub for you. Saying so keeps them out of
     * the "craftable" filter instead of promising something that will not happen.
     */
    @Override
    public boolean supportsRecipeTree() {
        return false;
    }
}
