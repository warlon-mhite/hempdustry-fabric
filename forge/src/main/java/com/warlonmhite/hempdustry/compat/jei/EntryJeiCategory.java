package com.warlonmhite.hempdustry.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.Arrays;
import java.util.List;
import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * One {@link ViewerRecipes.Entry} drawn as a JEI page: the inputs in a row, an arrow, the output,
 * and the entry's notes underneath.
 *
 * <p>One class serving every category — they differ only in their uid, title and icon, all of which
 * are constructor arguments. See {@link ViewerRecipes} for why every viewer reads one model.
 *
 * <p>JEI 7, the one 1.16.5 has, sizes a page by its category's background and fills its slots from
 * an {@link IIngredients} the category writes first; the newer lines' JEI asks the category for its
 * width and height and builds slots from a layout builder. The page is the same either way: the
 * background here is an empty drawable that answers the measured width.
 */
public class EntryJeiCategory implements IRecipeCategory<ViewerRecipes.Entry> {

    private static final int SLOT = 18;
    private static final int ARROW_WIDTH = 24;
    private static final int PADDING = 2;
    private static final int LINE_HEIGHT = 9;
    /** Widest an entry gets: the Infuser's four inputs. Fixed, so pages in a category line up. */
    private static final int MAX_INPUTS = 4;
    /** Inputs, arrow and output: the narrowest a page can be. */
    private static final int SLOTS_WIDTH = MAX_INPUTS * SLOT + ARROW_WIDTH + SLOT + PADDING * 2;
    /**
     * JEI 7's own sheet, and the empty arrow on it — the one its furnace page fills. Later JEIs name
     * the same picture {@code IGuiHelper#getRecipeArrow}; JEI 7 has no such call.
     */
    private static final ResourceLocation JEI_VANILLA_GUI = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");

    private final ResourceLocation uid;
    private final Component title;
    private final IDrawable icon;
    private final IDrawableStatic arrow;
    private final IDrawableStatic slot;
    private final IDrawable background = new IDrawable() {
        @Override
        public int getWidth() {
            return width();
        }

        @Override
        public int getHeight() {
            return SLOT + PADDING + noteLines * LINE_HEIGHT;
        }

        @Override
        public void draw(PoseStack poseStack, int xOffset, int yOffset) {
        }
    };
    private final int noteLines;
    /** This category's entries, kept so the notes can be measured in whatever language is on. */
    private List<ViewerRecipes.Entry> entries = ImmutableList.of();
    /** The width last measured, and the language it was measured in. */
    private int width = SLOTS_WIDTH;
    private String measuredIn = "";

    public EntryJeiCategory(IGuiHelper guiHelper, ResourceLocation id, ItemStack icon, int noteLines) {
        this.uid = id;
        this.title = new TranslatableComponent("hempdustry.category." + id.getPath());
        this.icon = guiHelper.createDrawableIngredient(icon);
        this.arrow = guiHelper.createDrawable(JEI_VANILLA_GUI, 24, 132, ARROW_WIDTH, 17);
        this.slot = guiHelper.getSlotDrawable();
        this.noteLines = noteLines;
    }

    @Override
    public ResourceLocation getUid() {
        return uid;
    }

    @Override
    public Class<? extends ViewerRecipes.Entry> getRecipeClass() {
        return ViewerRecipes.Entry.class;
    }

    @Override
    @Deprecated
    public String getTitle() {
        return title.getString();
    }

    @Override
    public Component getTitleAsTextComponent() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /**
     * The slots' width, or the widest note's if that is wider. Measured rather than fixed, because
     * a note is translated text: "Needs a heat source underneath" is half again as wide as the
     * slots in English, and a translation can be wider still. A fixed width ran every long note off
     * the recipe, and the Infuser's off the page. Re-measured when the language changes, since
     * switching it mid-game re-lays JEI's pages without registering the recipes again.
     */
    private int width() {
        Minecraft client = Minecraft.getInstance();
        String language = client.getLanguageManager().getSelected().getCode();
        if (!language.equals(measuredIn)) {
            Font font = client.font;
            int widest = entries.stream().flatMap(entry -> entry.notes().stream())
                    .mapToInt(font::width).max().orElse(0);
            width = Math.max(SLOTS_WIDTH, widest + 1); // notes are drawn from x = 1
            measuredIn = language;
        }
        return width;
    }

    /** Keeps the entries for {@link #width} and hands them straight back to be registered. */
    public List<ViewerRecipes.Entry> fitted(List<ViewerRecipes.Entry> entries) {
        this.entries = entries;
        this.measuredIn = "";
        return entries;
    }

    @Override
    public void setIngredients(ViewerRecipes.Entry entry, IIngredients ingredients) {
        ingredients.setInputIngredients(entry.inputs());
        ingredients.setOutput(VanillaTypes.ITEM, entry.output());
    }

    /**
     * JEI 7 places a slot by its 18-pixel frame, the item one pixel in: each frame is put a pixel up
     * and left of where the newer lines put the item, so the page lines up the same.
     */
    @Override
    public void setRecipe(IRecipeLayout layout, ViewerRecipes.Entry entry, IIngredients ingredients) {
        IGuiItemStackGroup stacks = layout.getItemStacks();
        int index = 0;
        int x = PADDING;
        for (Ingredient input : entry.inputs()) {
            stacks.init(index, true, x - 1, -1);
            stacks.setBackground(index, slot);
            stacks.set(index, Arrays.asList(input.getItems()));
            index++;
            x += SLOT;
        }
        // Measured from the widest entry rather than from this one, so the output column does not
        // jump between pages of the same category.
        stacks.init(index, false, PADDING + MAX_INPUTS * SLOT + ARROW_WIDTH - 1, -1);
        stacks.setBackground(index, slot);
        stacks.set(index, entry.output());
    }

    @Override
    public void draw(ViewerRecipes.Entry entry, PoseStack poseStack, double mouseX, double mouseY) {
        arrow.draw(poseStack, PADDING + MAX_INPUTS * SLOT + 4, 1);
        Font font = Minecraft.getInstance().font;
        int y = SLOT + PADDING;
        for (Component note : entry.notes()) {
            font.draw(poseStack, note, 1, y, 0x555555);
            y += LINE_HEIGHT;
        }
    }
}
