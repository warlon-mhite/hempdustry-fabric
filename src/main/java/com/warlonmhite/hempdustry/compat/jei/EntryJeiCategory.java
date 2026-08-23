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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    private final RecipeType<ViewerRecipes.Entry> type;
    private final Text title;
    private final IDrawable icon;
    private final IDrawableStatic arrow;
    private final IDrawableStatic slot;
    private final int noteLines;

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

    @Override
    public int getWidth() {
        return MAX_INPUTS * SLOT + ARROW_WIDTH + SLOT + PADDING * 2;
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
            context.drawText(MinecraftClient.getInstance().textRenderer, note, 1, y, 0x555555, false);
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
