package com.warlonmhite.hempdustry.screen.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Decarboxylator's screen. Follows vanilla's no-numbers convention (furnace flame, brewing
 * stand bubbles): the fire is a flame that burns down, and each tray shows its own filling arrow,
 * so three trays at different stages read at a glance without a single digit on screen.
 *
 * <p>The arrows are vanilla's own {@code burn_progress} sprite, drawn over the unlit silhouette
 * baked into the panel at the same origin. Vanilla's lit arrow sits one pixel above that silhouette
 * on purpose, so its shadow falls on the unlit shape's bottom and right edge — don't "align" them.
 */
public class DecarboxylatorScreen extends AbstractContainerScreen<DecarboxylatorScreenHandler> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Hempdustry.MOD_ID, "textures/gui/container/decarboxylator.png");

    // Sprite atlas regions inside the 256x256 texture, to the right of the 176x166 panel.
    private static final int FLAME_U = 176, FLAME_V = 0, FLAME_W = 14, FLAME_H = 14;
    private static final int ARROW_U = 176, ARROW_V = 14, ARROW_W = 24, ARROW_H = 16;

    /** Where the flame is drawn, just above the fuel slot. */
    private static final int FLAME_X = 26, FLAME_Y = 36;
    /** Where each tray's progress arrow starts — level with its slot, pointing right at the result. */
    private static final int ARROW_X = 85;

    public DecarboxylatorScreen(DecarboxylatorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(PoseStack context, float delta, int mouseX, int mouseY) {
        // 1.16.5 draws from whichever texture is bound, not from one named per call.
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.blit(context, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Fire: revealed from the bottom up, the way a furnace's flame burns down.
        if (this.menu.isBurning()) {
            int lit = Math.round(this.menu.getBurnProgress() * FLAME_H);
            if (lit > 0) {
                this.blit(context, x + FLAME_X, y + FLAME_Y + FLAME_H - lit,
                        FLAME_U, FLAME_V + FLAME_H - lit,
                        FLAME_W, lit);
            }
        }

        // One arrow per tray, each filling left-to-right on its own timer, level with its slot.
        for (int tray = 0; tray < DecarboxylatorBlockEntity.TRAY_COUNT; tray++) {
            int filled = Math.round(this.menu.getCookProgress(tray) * ARROW_W);
            if (filled > 0) {
                int arrowY = y + DecarboxylatorScreenHandler.TRAY_Y
                        + tray * DecarboxylatorScreenHandler.TRAY_SPACING;
                this.blit(context, x + ARROW_X, arrowY, ARROW_U, ARROW_V, filled, ARROW_H);
            }
        }
    }

    @Override
    public void render(PoseStack context, int mouseX, int mouseY, float delta) {
        // The dimmed world behind the panel, which 1.20.2 and later draw for every screen.
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.renderTooltip(context, mouseX, mouseY);
    }
}
