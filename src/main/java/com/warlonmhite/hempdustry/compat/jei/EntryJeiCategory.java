package com.warlonmhite.hempdustry.compat.jei;

import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * One {@link ViewerRecipes.Entry} drawn as a JEI page: the inputs in a row, an arrow, the output,
 * and the entry's notes underneath.
 *
 * <p>One class serving all three categories — they differ only in their {@link RecipeType}, title
 * and icon, all of which are constructor arguments. The layout maths is the same for each and the
 * EMI side draws it identically; see {@link ViewerRecipes} for why both viewers read one model.
 */
public class EntryJeiCategory implements IRecipeCategory<ViewerRecipes.Entry> {

    private static final int SLOT = 18;
    private static final int ARROW_WIDTH = 24;
    private static final int PADDING = 2;
    private static final int LINE_HEIGHT = 9;
    /** Widest an entry gets: the Infuser's three inputs. Fixed, so pages in a category line up. */
    private static final int MAX_INPUTS = 3;
    /** Inputs, arrow and output: the narrowest a page can be. */
    private static final int SLOTS_WIDTH = MAX_INPUTS * SLOT + ARROW_WIDTH + SLOT + PADDING * 2;
    /** The note lines' grey, opaque: an ARGB colour with no alpha byte is drawn as nothing. */
    private static final int NOTE_COLOR = 0xFF555555;

    private final RecipeType<ViewerRecipes.Entry> type;
    private final Text title;
    private final IDrawable icon;
    private final IDrawableStatic arrow;
    private final IDrawableStatic slot;
    private final int noteLines;
    /** This category's entries, kept so the notes can be measured in whatever language is on. */
    private List<ViewerRecipes.Entry> entries = List.of();
    /** The width last measured, and the language it was measured in. */
    private int width = SLOTS_WIDTH;
    private String measuredIn = "";

    public EntryJeiCategory(IGuiHelper guiHelper, Identifier id, ItemStack icon, int noteLines) {
        this.type = new RecipeType<>(id, ViewerRecipes.Entry.class);
        this.title = Text.translatable("hempdustry.category." + id.getPath());
        this.icon = guiHelper.createDrawableItemStack(icon);
        this.arrow = guiHelper.getRecipeArrow();
        this.slot = guiHelper.getSlotDrawable();
        this.noteLines = noteLines;
    }

    @Override
    public RecipeType<ViewerRecipes.Entry> getRecipeType() {
        return type;
    }

    @Override
    public Text getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /**
     * The slots' width, or the widest note's if that is wider. Measured rather than fixed, because
     * a note is translated text: "Needs a heat source underneath" is half again as wide as the
     * slots in English, and a translation can be wider still. A fixed width ran every long note off
     * the recipe, and the Infuser's off the page -- unnoticed while the notes were drawn transparent.
     * Re-measured when the language changes, since switching it mid-game re-lays JEI's pages
     * without registering the recipes again.
     */
    @Override
    public int getWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        String language = client.getLanguageManager().getLanguage();
        if (!language.equals(measuredIn)) {
            TextRenderer font = client.textRenderer;
            int widest = entries.stream().flatMap(entry -> entry.notes().stream())
                    .mapToInt(font::getWidth).max().orElse(0);
            width = Math.max(SLOTS_WIDTH, widest + 1); // notes are drawn from x = 1
            measuredIn = language;
        }
        return width;
    }

    /** Keeps the entries for {@link #getWidth} and hands them straight back to be registered. */
    public List<ViewerRecipes.Entry> fitted(List<ViewerRecipes.Entry> entries) {
        this.entries = entries;
        this.measuredIn = "";
        return entries;
    }

    @Override
    public int getHeight() {
        return SLOT + PADDING + noteLines * LINE_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ViewerRecipes.Entry entry, IFocusGroup focuses) {
        int x = PADDING;
        for (Ingredient input : entry.inputs()) {
            builder.addSlot(RecipeIngredientRole.INPUT, x, 0)
                    .setBackground(slot, -1, -1)
                    .addIngredients(input);
            x += SLOT;
        }
        // Measured from the widest entry rather than from this one, so the output column does not
        // jump between pages of the same category.
        builder.addSlot(RecipeIngredientRole.OUTPUT, PADDING + MAX_INPUTS * SLOT + ARROW_WIDTH, 0)
                .setBackground(slot, -1, -1)
                .addItemStack(entry.output());
    }

    @Override
    public void draw(ViewerRecipes.Entry entry, IRecipeSlotsView slots, DrawContext context,
                     double mouseX, double mouseY) {
        arrow.draw(context, PADDING + MAX_INPUTS * SLOT + 4, 1);
        int y = SLOT + PADDING;
        for (Text note : entry.notes()) {
            // ARGB, and the alpha byte is not optional: since 1.21.6 drawText skips a colour whose
            // alpha is 0, so a bare 0x555555 drew every note on every page as nothing at all.
            context.drawText(MinecraftClient.getInstance().textRenderer, note, 1, y, NOTE_COLOR, false);
            y += LINE_HEIGHT;
        }
    }

    /**
     * JEI uses this to name a recipe in its own bookkeeping — the "show recipe id" debug line and
     * the hidden-recipe config both key on it. The entries already carry a unique identifier, so
     * handing it over costs nothing and makes them addressable.
     */
    @Override
    public Identifier getRegistryName(ViewerRecipes.Entry entry) {
        return entry.id();
    }
}
