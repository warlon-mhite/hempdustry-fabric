package com.warlonmhite.hempdustry.compat.rei;

import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

/**
 * One {@link ViewerRecipes.Entry} as a REI display.
 *
 * <p>{@link BasicDisplay} already carries inputs, outputs and a location; this adds the two things
 * it cannot know — which category the row belongs to, and the note lines {@link EntryReiCategory}
 * draws under the slots.
 */
public class EntryReiDisplay extends BasicDisplay {

    private final CategoryIdentifier<?> category;
    private final List<Text> notes;

    public EntryReiDisplay(CategoryIdentifier<?> category, ViewerRecipes.Entry entry) {
        super(EntryIngredients.ofIngredients(entry.inputs()),
                List.of(EntryIngredients.of(entry.output())),
                // A synthetic row has no recipe behind it, so it has no location. REI treats an
                // empty Optional as exactly that, which is tidier than EMI's leading-slash
                // convention and means the same thing: nothing here has gone missing.
                entry.synthetic() ? Optional.empty() : Optional.of(entry.id()));
        this.category = category;
        this.notes = entry.notes();
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    /** The lines {@link EntryReiCategory} draws under the slots. */
    public List<Text> notes() {
        return notes;
    }
}
