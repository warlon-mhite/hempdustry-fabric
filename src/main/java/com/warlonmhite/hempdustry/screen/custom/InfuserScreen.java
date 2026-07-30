package com.warlonmhite.hempdustry.screen.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The Infuser's screen.
 *
 * <p>The bar is the interesting part. A plain fill bar would say "this is cooking" but not the thing
 * the player actually needs to know, which is that <em>there is a better outcome if you wait</em>.
 * So it carries a **notch** at the early-pull minimum: fill reaching the notch is when cannabutter
 * appears in the output slot at all, and the stretch beyond it is the part you're choosing to skip
 * if you take it now. Two signals saying the same thing — something collectable in the slot, and a
 * bar that plainly is not finished — with no numbers anywhere, per vanilla's convention.
 */
public class InfuserScreen extends HandledScreen<InfuserScreenHandler> {
    private static final Identifier TEXTURE =
            Identifier.of(Hempdustry.MOD_ID, "textures/gui/container/infuser.png");

    // Sprite regions in the 256x256 sheet, right of the 176x166 panel.
    private static final int BAR_U = 176, BAR_V = 0, BAR_W = 44, BAR_H = 5;
    private static final int NOTCH_U = 176, NOTCH_V = 5, NOTCH_W = 2, NOTCH_H = 7;
    private static final int FLAME_U = 180, FLAME_V = 5, FLAME_W = 14, FLAME_H = 14;

    /**
     * The bar runs between the hemp slots and the collection slot, where a furnace puts its arrow.
     * Kept well clear of y=72, which is where the "Inventory" label sits on a 166-tall panel.
     */
    private static final int BAR_X = 84, BAR_Y = 39;
    /** Heat indicator, under the milk slot. Reports the block below, not a fuel level. */
    private static final int FLAME_X = 26, FLAME_Y = 56;

    public InfuserScreen(InfuserScreenHandler handler, PlayerInventory inventory, Text title) {
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
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // Simmer bar, filling left to right.
        int filled = Math.round(this.handler.getProgress() * BAR_W);
        if (filled > 0) {
            context.drawTexture(TEXTURE, x + BAR_X, y + BAR_Y, BAR_U, BAR_V, filled, BAR_H);
        }

        // The notch: everything left of it is "ready, but Rough"; everything right of it is the
        // patience you're buying. Drawn over the fill so it stays legible either way.
        int notchX = x + BAR_X + Math.round(this.handler.getMinimumMark() * BAR_W) - (NOTCH_W / 2);
        context.drawTexture(TEXTURE, notchX, y + BAR_Y - 1, NOTCH_U, NOTCH_V, NOTCH_W, NOTCH_H);

        // Heat indicator. Not a fuel gauge — it reports whether something hot is underneath.
        if (this.handler.isHeated()) {
            context.drawTexture(TEXTURE, x + FLAME_X, y + FLAME_Y, FLAME_U, FLAME_V, FLAME_W, FLAME_H);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
