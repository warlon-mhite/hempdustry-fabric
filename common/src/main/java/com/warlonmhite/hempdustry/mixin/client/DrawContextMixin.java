package com.warlonmhite.hempdustry.mixin.client;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Draws the smoking cooldown swipe on the stack that was actually smoked, and on nothing else.
 *
 * <p>{@code ItemCooldowns} is keyed by {@code Item}, and a hit deliberately arms it on every
 * smokeable at once (see {@link Smoking#startCooldown} for why the <em>block</em> has to be global).
 * The overlay in {@code ItemRenderer#renderGuiItemDecorations} asks that manager one question per
 * slot — "is this item cooling down?" — so straight off vanilla it sweeps a white bar down every pipe,
 * every bong and every spliff in the inventory, including the empty ones that were never smoked and
 * could not have been. That reads as a bug even though the cooldown underneath it is correct.
 *
 * <p>The mark that tells one stack from another is {@link ModComponents#COOLDOWN_UNTIL}, set on the
 * used stack and cleared when it lapses. Only smokeables are gated on it: every other cooling item
 * in the game — ender pearls, chorus fruit, another mod's — keeps the vanilla overlay untouched.
 *
 * <h2>Why {@code @ModifyVariable}</h2>
 *
 * It composes. {@code @Redirect} would replace the cooldown call outright and collide with any other
 * mod touching the same instruction, a start-up crash under strict mixins; this only adjusts the
 * value once it is stored, so several mods can stack on it. On 1.16.5 the overlay draws from the
 * item renderer rather than a {@code GuiGraphics}, and the swipe's fraction is the method's only
 * {@code float} local, so the store is unambiguous. The newer lines do the same with MixinExtras,
 * which Forge 36 cannot carry.
 */
@Mixin(ItemRenderer.class)
public class DrawContextMixin {
    @ModifyVariable(
            method = "renderGuiItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("STORE"),
            ordinal = 0
    )
    private float hempdustry$swipeOnlyTheStackThatWasSmoked(float progress, Font font, ItemStack stack) {
        if (progress > 0.0F && Smoking.isSmokeable(stack.getItem())
                && !ModComponents.COOLDOWN_UNTIL.has(stack)) {
            return 0.0F;
        }
        return progress;
    }
}
