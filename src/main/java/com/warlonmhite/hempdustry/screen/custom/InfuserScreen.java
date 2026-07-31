package com.warlonmhite.hempdustry.screen.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * The Infuser's screen.
 *
 * <p>The bar is the interesting part, and it measures <b>this batch's job rather than the clock</b>.
 * Only an all-washed batch runs the full timer — Perfect is the one grade gated on it — so scaling to
 * {@code FULL_TIME} meant the bar never filled for anything else, throwing away the most learnable
 * thing a progress bar has: <b>full means done</b>. It now reaches the end exactly when the batch
 * reaches the best grade it can, which is also when the spout pours it.
 *
 * <p>Two marks ride on it, both drawn here at runtime because both move with the washed ratio:
 * <ul>
 *   <li>a <b>dark notch</b> at the early-pull minimum — fill reaching it is when cannabutter appears
 *       in the output slot at all. Everything left of it is "nothing to take yet".</li>
 *   <li>a <b>bright mark</b> at the next grade up — the answer to "should I wait". The last upgrade's
 *       mark sits on the bar's end, so a full bar and a finished batch are the same sight, and once
 *       there is nothing left to wait for the mark simply stops being drawn.</li>
 * </ul>
 * No numbers anywhere, per vanilla's convention.
 */
public class InfuserScreen extends HandledScreen<InfuserScreenHandler> {
    private static final Identifier TEXTURE =
            Identifier.of(Hempdustry.MOD_ID, "textures/gui/container/infuser.png");

    // Sprite regions in the 256x256 sheet, right of the 176x166 panel.
    private static final int BAR_U = 176, BAR_V = 0, BAR_W = 44, BAR_H = 5;
    private static final int NOTCH_U = 176, NOTCH_V = 5, NOTCH_W = 2, NOTCH_H = 7;
    private static final int MARK_U = 178, MARK_V = 5, MARK_W = 2, MARK_H = 7;
    private static final int FLAME_U = 180, FLAME_V = 5, FLAME_W = 14, FLAME_H = 14;

    /**
     * The bar runs between the hemp slots and the collection slot, where a furnace puts its arrow.
     * Kept well clear of y=72, which is where the "Inventory" label sits on a 166-tall panel.
     */
    private static final int BAR_X = 84, BAR_Y = 39;
    /**
     * Heat indicator, between the milk slot and the bucket return — a furnace's fire position, in a
     * furnace's column. It still reports the block <em>below</em> the Infuser rather than a fuel
     * level; borrowing the position borrows the reading "this is what makes it go".
     */
    private static final int FLAME_X = 28, FLAME_Y = 37;

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

        // Both marks are drawn at runtime rather than baked into the panel, because the bar is scaled
        // to *this* batch's job rather than to the clock — so both of them move with the washed ratio.

        // The dark notch: everything left of it is "nothing collectable yet". Drawn over the fill so
        // it stays legible either way.
        drawMark(context, x, y, this.handler.getMinimumMark(), NOTCH_U, NOTCH_V, NOTCH_W, NOTCH_H);

        // The bright mark: where this batch earns its next grade up. Two marks, two different
        // questions — the dark one is "can I take it", the bright one is "should I wait". The final
        // upgrade's mark lands on the bar's end, so a full bar and a finished batch are the same
        // sight; once there is nothing left to wait for the mark simply stops being drawn.
        float nextGrade = this.handler.getNextGradeMark();
        if (nextGrade >= 0.0F) {
            drawMark(context, x, y, nextGrade, MARK_U, MARK_V, MARK_W, MARK_H);
        }

        // Heat indicator. Not a fuel gauge — it reports whether something hot is underneath.
        if (this.handler.isHeated()) {
            context.drawTexture(TEXTURE, x + FLAME_X, y + FLAME_Y, FLAME_U, FLAME_V, FLAME_W, FLAME_H);
        }
    }

    /** Places a mark sprite at {@code fraction} along the bar, clamped so it never overhangs the end. */
    private void drawMark(DrawContext context, int x, int y, float fraction,
                          int u, int v, int width, int height) {
        int offset = MathHelper.clamp(Math.round(fraction * BAR_W) - (width / 2), 0, BAR_W - width);
        context.drawTexture(TEXTURE, x + BAR_X + offset, y + BAR_Y - 1, u, v, width, height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
