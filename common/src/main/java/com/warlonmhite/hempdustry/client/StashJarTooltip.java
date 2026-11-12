package com.warlonmhite.hempdustry.client;

import com.warlonmhite.hempdustry.component.StashContents;
import com.warlonmhite.hempdustry.item.custom.StashJarItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Stash Jar's contents in its tooltip, drawn as vanilla draws a bundle's
 * ({@code ClientBundleTooltip}): the same grid of the last stacks put in, the same "+n" for the rest,
 * the selected stack's name above, the same bar and sprites. What changes: the bar measures items
 * against this jar's capacity, where a bundle's measures weight against one stack; each slot is one
 * strain, its best buds drawn with the whole count; and under the bar one line per strain shown says
 * exactly what it will give back ({@link StashJarItem#entryLine}).
 */
public final class StashJarTooltip implements ClientTooltipComponent {
    private static final Identifier PROGRESSBAR_BORDER = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_border");
    private static final Identifier PROGRESSBAR_FILL = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_fill");
    private static final Identifier PROGRESSBAR_FULL = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_full");
    private static final Identifier SLOT_HIGHLIGHT_BACK = Identifier.withDefaultNamespace("container/bundle/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");
    private static final Identifier SLOT_BACKGROUND = Identifier.withDefaultNamespace("container/bundle/slot_background");
    private static final int SLOT_SIZE = 24;
    private static final int GRID_WIDTH = 96;
    private static final int PROGRESSBAR_HEIGHT = 13;
    private static final int PROGRESSBAR_FILL_MAX = 94;
    private static final int MARGIN_Y = 4;
    private static final int LINE_HEIGHT = 10;
    private static final Component FULL = Component.translatable("item.minecraft.bundle.full");
    private static final Component EMPTY = Component.translatable("item.minecraft.bundle.empty");
    private static final Component EMPTY_DESCRIPTION = Component.translatable("item.hempdustry.stash_jar.empty.description");

    private final StashContents contents;
    private final int capacity;
    private final List<Component> lines;

    public StashJarTooltip(StashJarItem.StashTooltip tooltip) {
        this.contents = tooltip.contents();
        this.capacity = tooltip.capacity();
        this.lines = contents.entries().subList(0, contents.shown()).stream()
                .map(entry -> (Component) StashJarItem.entryLine(entry).copy().withStyle(ChatFormatting.GRAY))
                .toList();
    }

    @Override
    public int getHeight(Font font) {
        return contents.isEmpty() ? emptyDescriptionHeight(font) + PROGRESSBAR_HEIGHT + 2 * MARGIN_Y
                : gridHeight() + PROGRESSBAR_HEIGHT + 2 * MARGIN_Y + lines.size() * LINE_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int width = GRID_WIDTH;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        return width;
    }

    @Override
    public boolean showTooltipWithItemInHand() {
        return true;
    }

    private int gridHeight() {
        return gridRows() * SLOT_SIZE;
    }

    private int gridRows() {
        return Mth.positiveCeilDiv(Math.min(12, contents.size()), 4);
    }

    private static int contentX(int tooltipWidth) {
        return (tooltipWidth - GRID_WIDTH) / 2;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int left = x + contentX(w);
        if (contents.isEmpty()) {
            graphics.textWithWordWrap(font, EMPTY_DESCRIPTION, left, y, GRID_WIDTH, -5592406);
            progressBar(left, y + emptyDescriptionHeight(font) + MARGIN_Y, font, graphics);
            return;
        }
        boolean overflowing = contents.size() > 12;
        List<StashContents.Entry> shown = contents.entries().subList(0, Math.min(contents.size(), contents.shown()));
        int xStart = left + GRID_WIDTH;
        int yStart = y + gridRows() * SLOT_SIZE;
        int slot = 1;
        for (int row = 1; row <= gridRows(); row++) {
            for (int column = 1; column <= 4; column++) {
                int drawX = xStart - column * SLOT_SIZE;
                int drawY = yStart - row * SLOT_SIZE;
                if (overflowing && column * row == 1) {
                    int hidden = contents.entries().stream().skip(shown.size()).mapToInt(StashContents.Entry::count).sum();
                    graphics.centeredText(font, "+" + hidden, drawX + 12, drawY + 10, -1);
                } else if (shown.size() >= slot) {
                    slot(slot, drawX, drawY, shown, font, graphics);
                    slot++;
                }
            }
        }
        selectedName(font, graphics, x, y, w);
        progressBar(left, y + gridHeight() + MARGIN_Y, font, graphics);
        int lineY = y + gridHeight() + PROGRESSBAR_HEIGHT + 2 * MARGIN_Y;
        for (Component line : lines) {
            graphics.text(font, line, x, lineY, -1);
            lineY += LINE_HEIGHT;
        }
    }

    private void slot(int slot, int drawX, int drawY, List<StashContents.Entry> shown, Font font, GuiGraphicsExtractor graphics) {
        int index = shown.size() - slot;
        boolean highlighted = index == contents.selected();
        StashContents.Entry entry = shown.get(index);
        ItemStack item = entry.icon();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, highlighted ? SLOT_HIGHLIGHT_BACK : SLOT_BACKGROUND,
                drawX, drawY, SLOT_SIZE, SLOT_SIZE);
        graphics.item(item, drawX + 4, drawY + 4, slot);
        // The icon carries the whole strain's count, which may pass a stack.
        graphics.itemDecorations(font, item, drawX + 4, drawY + 4);
        if (highlighted) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT, drawX, drawY, SLOT_SIZE, SLOT_SIZE);
        }
    }

    private void selectedName(Font font, GuiGraphicsExtractor graphics, int x, int y, int w) {
        StashContents.Entry selected = contents.selectedEntry();
        if (selected == null) {
            return;
        }
        ItemStack stack = selected.icon();
        Component name = stack.getStyledHoverName();
        int textWidth = font.width(name.getVisualOrderText());
        int centre = x + w / 2 - 12;
        graphics.tooltip(font, List.of(ClientTooltipComponent.create(name.getVisualOrderText())),
                centre - textWidth / 2, y - 15, DefaultTooltipPositioner.INSTANCE,
                stack.get(DataComponents.TOOLTIP_STYLE), false);
    }

    private void progressBar(int x, int y, Font font, GuiGraphicsExtractor graphics) {
        float fullness = contents.fullness(capacity);
        boolean full = contents.count() >= capacity;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, full ? PROGRESSBAR_FULL : PROGRESSBAR_FILL, x + 1, y,
                Mth.clamp((int) (fullness * PROGRESSBAR_FILL_MAX), 0, PROGRESSBAR_FILL_MAX), PROGRESSBAR_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER, x, y, GRID_WIDTH, PROGRESSBAR_HEIGHT);
        @Nullable Component text = contents.isEmpty() ? EMPTY : full ? FULL : null;
        if (text != null) {
            graphics.centeredText(font, text, x + GRID_WIDTH / 2, y + 3, -1);
        }
    }

    private static int emptyDescriptionHeight(Font font) {
        return font.split(EMPTY_DESCRIPTION, GRID_WIDTH).size() * 9;
    }
}
