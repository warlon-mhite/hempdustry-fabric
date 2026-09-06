package com.warlonmhite.hempdustry.compat.rei;

import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * One {@link EntryReiDisplay} drawn as a REI page: the inputs in a row, an arrow, the output, and
 * the display's notes underneath.
 *
 * <p>One class serving all three categories — they differ only in their identifier, title and icon,
 * all of which are constructor arguments. This is the same layout the JEI and EMI sides draw, from
 * the same model; see {@link ViewerRecipes} for why all three viewers read one.
 */
public class EntryReiCategory implements DisplayCategory<EntryReiDisplay> {

    private static final int SLOT = 18;
    private static final int ARROW_WIDTH = 24;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 10;
    /** Widest an entry gets: the Infuser's three inputs. Fixed, so pages in a category line up. */
    private static final int MAX_INPUTS = 3;
    /** REI's own note colours, light theme then dark — {@code DefaultFuelCategory} uses this pair. */
    private static final int NOTE_LIGHT = 0xFF404040;
    private static final int NOTE_DARK = 0xFFBBBBBB;

    private final CategoryIdentifier<EntryReiDisplay> id;
    private final Text title;
    private final Renderer icon;
    private final int noteLines;

    public EntryReiCategory(Identifier id, ItemConvertible icon, int noteLines) {
        this.id = CategoryIdentifier.of(id);
        // The same key JEI's and EMI's titles come from, rather than REI's default
        // category.rei.<namespace>.<path> — one set of titles across eight locales, and no two
        // viewers can end up saying different words for the same category.
        this.title = Text.translatable("hempdustry.category." + id.getPath());
        this.icon = EntryStacks.of(icon);
        this.noteLines = noteLines;
    }

    @Override
    public CategoryIdentifier<? extends EntryReiDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Text getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayWidth(EntryReiDisplay display) {
        return MAX_INPUTS * SLOT + ARROW_WIDTH + SLOT + PADDING * 2;
    }

    @Override
    public int getDisplayHeight() {
        return SLOT + PADDING + noteLines * LINE_HEIGHT + PADDING * 2;
    }

    @Override
    public List<Widget> setupDisplay(EntryReiDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        int x = bounds.x + PADDING;
        int y = bounds.y + PADDING;
        for (EntryIngredient input : display.getInputEntries()) {
            widgets.add(Widgets.createSlot(new Point(x, y)).entries(input).markInput());
            x += SLOT;
        }
        // Measured from the widest entry rather than from this one, so the output column does not
        // jump between pages of the same category.
        int outputX = bounds.x + PADDING + MAX_INPUTS * SLOT + ARROW_WIDTH;
        widgets.add(Widgets.createArrow(new Point(bounds.x + PADDING + MAX_INPUTS * SLOT, y + 1)));
        widgets.add(Widgets.createSlot(new Point(outputX, y))
                .entries(display.getOutputEntries().getFirst()).markOutput());

        int noteY = y + SLOT + PADDING;
        for (Text note : display.notes()) {
            widgets.add(Widgets.createLabel(new Point(bounds.x + PADDING, noteY), note)
                    .color(NOTE_LIGHT, NOTE_DARK).noShadow().leftAligned());
            noteY += LINE_HEIGHT;
        }
        return widgets;
    }
}
