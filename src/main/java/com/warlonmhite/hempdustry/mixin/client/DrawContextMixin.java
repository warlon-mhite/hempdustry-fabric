package com.warlonmhite.hempdustry.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws the smoking cooldown swipe on the stack that was actually smoked, and on nothing else.
 *
 * <p>{@code ItemCooldownManager} is keyed by a cooldown <em>group</em> — the item's own id unless a
 * {@code use_cooldown} component says otherwise — and a hit deliberately arms it on every
 * smokeable at once (see {@link Smoking#startCooldown} for why the <em>block</em> has to be global).
 * The overlay in {@code DrawContext#drawCooldownProgress} asks that manager one question per slot —
 * "is this item cooling down?" — so straight off vanilla it sweeps a white bar down every pipe,
 * every bong and every spliff in the inventory, including the empty ones that were never smoked and
 * could not have been. That reads as a bug even though the cooldown underneath it is correct.
 *
 * <p>The mark that tells one stack from another is {@link ModComponents#COOLDOWN_UNTIL}, set on the
 * used stack and cleared when it lapses. Only smokeables are gated on it: every other cooling item
 * in the game — ender pearls, chorus fruit, another mod's — keeps the vanilla overlay untouched.
 *
 * <h2>Why {@code @ModifyExpressionValue}</h2>
 *
 * It composes. {@code @Redirect} would replace the call outright and collide with any other mod
 * touching the same instruction (CLAUDE.md §5); this only adjusts the value that came back, so
 * several mods can stack on it. MixinExtras ships inside Fabric Loader, so it costs no dependency —
 * same reasoning as {@code BeeGrowCropsGoalMixin}.
 */
@Mixin(DrawContext.class)
public class DrawContextMixin {
    @ModifyExpressionValue(
            method = "drawCooldownProgress(Lnet/minecraft/item/ItemStack;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;getCooldownProgress(Lnet/minecraft/item/ItemStack;F)F"
            )
    )
    private float hempdustry$swipeOnlyTheStackThatWasSmoked(float progress,
                                                            @Local(argsOnly = true) ItemStack stack) {
        if (progress > 0.0F && Smoking.isSmokeable(stack.getItem())
                && !stack.contains(ModComponents.COOLDOWN_UNTIL)) {
            return 0.0F;
        }
        return progress;
    }
}
