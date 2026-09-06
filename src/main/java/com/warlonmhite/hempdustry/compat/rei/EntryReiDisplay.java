package com.warlonmhite.hempdustry.compat.rei;

import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.Display;
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

    /**
     * <b>Null on purpose</b>, which is REI's way of saying a display cannot be serialized.
     *
     * <p>A serializer is for displays that travel — synced from a server, or written into the
     * favourites and history files. These are built on the client from the recipe manager and from
     * {@code ModCauldronBehaviors}' own constants, and are rebuilt on every reload, so there is
     * nothing to gain by making them persistable. REI null-checks before every use of it
     * ({@code DisplayHistoryManager}, {@code EntryStacksRegionWidget}); the cost of returning null
     * is that a player cannot pin one of these rows to their favourites.
     */
    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
