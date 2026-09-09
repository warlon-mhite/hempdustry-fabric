package com.warlonmhite.hempdustry.screen.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The Hemp Press's screen: input, arrow, output, and a flame under the arrow.
 *
 * <p>The flame is <b>not</b> a fuel gauge — nothing here burns. It reports whether the block below
 * the press is hot, which is a fact no arrangement of slots can express, so it sits directly under
 * the arrow it governs. Same placement and the same reasoning as the Infuser's.
 */
public class HempPressScreen extends HandledScreen<HempPressScreenHandler> {
    private static final Identifier TEXTURE =
            Identifier.of(Hempdustry.MOD_ID, "textures/gui/container/hemp_press.png");

    // Sprite regions in the 256x256 sheet, right of the 176x166 panel.
    private static final int ARROW_U = 176, ARROW_V = 0, ARROW_W = 24, ARROW_H = 16;
    private static final int FLAME_U = 176, FLAME_V = 16, FLAME_W = 14, FLAME_H = 14;

    private static final int ARROW_X = 79, ARROW_Y = 35;
    /** Centred under the arrow (79 + 24/2 = 91, less half the flame's 14). */
    private static final int FLAME_X = 84, FLAME_Y = 55;

    public HempPressScreen(HempPressScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleY = 6;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, 256, 256);

        int filled = Math.round(this.handler.getProgress() * ARROW_W);
        if (filled > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y,
                    ARROW_U, ARROW_V, filled, ARROW_H, 256, 256);
        }

        if (this.handler.isHeated()) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + FLAME_X, y + FLAME_Y,
                    FLAME_U, FLAME_V, FLAME_W, FLAME_H, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
