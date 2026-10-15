package com.warlonmhite.hempdustry.screen.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

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
public class InfuserScreen extends AbstractContainerScreen<InfuserScreenHandler> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "textures/gui/container/infuser.png");

    // Sprite regions in the 256x256 sheet, right of the 176x166 panel.
    private static final int BAR_U = 176, BAR_V = 0, BAR_W = 44, BAR_H = 5;
    private static final int NOTCH_U = 176, NOTCH_V = 5, NOTCH_W = 2, NOTCH_H = 7;
    private static final int MARK_U = 178, MARK_V = 5, MARK_W = 2, MARK_H = 7;
    private static final int FLAME_U = 180, FLAME_V = 5, FLAME_W = 14, FLAME_H = 14;
    private static final int MILK_U = 194, MILK_V = 5, MILK_W = 14, MILK_H = 12;

    /**
     * Milk indicator: an empty pot outline, filled white once milk is poured in. It stands where the
     * milk slot used to, in the left column, as the first of the batch's two preconditions — milk,
     * then heat — which is why it is a picture like the flame and not a slot: nothing goes in here.
     */
    private static final int MILK_X = 27, MILK_Y = 37;

    /**
     * The bar runs between the hemp slots and the collection slot, where a furnace puts its arrow.
     * Kept well clear of y=72, which is where the "Inventory" label sits on a 166-tall panel.
     */
    private static final int BAR_X = 84, BAR_Y = 39;
    /**
     * Heat indicator, centred under the bar and on the row of the bottom two slots.
     *
     * <p>It used to sit between the old milk slot and bucket return, borrowing a furnace's
     * fire-in-the-middle-of-the-column arrangement. That read as a lie: this flame is not a fuel
     * gauge and has nothing to do with either bucket — it reports whether the block <em>below</em>
     * the Infuser is hot. Sitting it directly beneath the simmer bar puts it next to the one thing
     * it actually governs, so "no heat, no progress" is legible without a tooltip.
     */
    private static final int FLAME_X = 99, FLAME_Y = 54;

    public InfuserScreen(InfuserScreenHandler handler, Inventory inventory, Component title) {
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
        context.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Simmer bar, filling left to right.
        int filled = Math.round(this.menu.getProgress() * BAR_W);
        if (filled > 0) {
            context.blit(TEXTURE, x + BAR_X, y + BAR_Y, BAR_U, BAR_V, filled, BAR_H);
        }

        // Both marks are drawn at runtime rather than baked into the panel, because the bar is scaled
        // to *this* batch's job rather than to the clock — so both of them move with the washed ratio.

        // The dark notch: everything left of it is "nothing collectable yet". Drawn over the fill so
        // it stays legible either way.
        drawMark(context, x, y, this.menu.getMinimumMark(), NOTCH_U, NOTCH_V, NOTCH_W, NOTCH_H);

        // The bright mark: where this batch earns its next grade up. Two marks, two different
        // questions — the dark one is "can I take it", the bright one is "should I wait". The final
        // upgrade's mark lands on the bar's end, so a full bar and a finished batch are the same
        // sight; once there is nothing left to wait for the mark simply stops being drawn.
        float nextGrade = this.menu.getNextGradeMark();
        if (nextGrade >= 0.0F) {
            drawMark(context, x, y, nextGrade, MARK_U, MARK_V, MARK_W, MARK_H);
        }

        if (this.menu.isFilled()) {
            context.blit(TEXTURE, x + MILK_X, y + MILK_Y, MILK_U, MILK_V, MILK_W, MILK_H);
        }

        // Heat indicator. Not a fuel gauge — it reports whether something hot is underneath.
        if (this.menu.isHeated()) {
            context.blit(TEXTURE, x + FLAME_X, y + FLAME_Y, FLAME_U, FLAME_V, FLAME_W, FLAME_H);
        }
    }

    /** Places a mark sprite at {@code fraction} along the bar, clamped so it never overhangs the end. */
    private void drawMark(GuiGraphics context, int x, int y, float fraction,
                          int u, int v, int width, int height) {
        int offset = Mth.clamp(Math.round(fraction * BAR_W) - (width / 2), 0, BAR_W - width);
        context.blit(TEXTURE, x + BAR_X + offset, y + BAR_Y - 1, u, v, width, height);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderTooltip(context, mouseX, mouseY);
    }
}
