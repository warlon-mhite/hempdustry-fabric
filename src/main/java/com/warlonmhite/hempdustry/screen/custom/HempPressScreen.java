package com.warlonmhite.hempdustry.screen.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Hemp Press's screen: input, arrow, output, and a flame under the arrow.
 *
 * <p>The flame is <b>not</b> a fuel gauge — nothing here burns. It reports whether the block below
 * the press is hot, which is a fact no arrangement of slots can express, so it sits directly under
 * the arrow it governs. Same placement and the same reasoning as the Infuser's.
 */
public class HempPressScreen extends AbstractContainerScreen<HempPressScreenHandler> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "textures/gui/container/hemp_press.png");

    // Sprite regions in the 256x256 sheet, right of the 176x166 panel.
    private static final int ARROW_U = 176, ARROW_V = 0, ARROW_W = 24, ARROW_H = 16;
    private static final int FLAME_U = 176, FLAME_V = 16, FLAME_W = 14, FLAME_H = 14;

    private static final int ARROW_X = 79, ARROW_Y = 35;
    /** Centred under the arrow (79 + 24/2 = 91, less half the flame's 14). */
    private static final int FLAME_X = 84, FLAME_Y = 55;

    public HempPressScreen(HempPressScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);

        int filled = Math.round(this.menu.getProgress() * ARROW_W);
        if (filled > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y,
                    ARROW_U, ARROW_V, filled, ARROW_H, 256, 256);
        }

        if (this.menu.isHeated()) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + FLAME_X, y + FLAME_Y,
                    FLAME_U, FLAME_V, FLAME_W, FLAME_H, 256, 256);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderTooltip(context, mouseX, mouseY);
    }
}
