package com.warlonmhite.hempdustry.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Averted Gaze: while it lasts, an enderman treats you as if you wore a carved pumpkin — you can
 * look it in the eye and it does not see you — and harvesting Ender Haze wakes no enderman. It does
 * nothing on its own; {@code EnderManMixin} and {@code EnderHazeCropBlock#provoke} read it.
 *
 * <p>Ender Haze's signature, and the consumable half of a vanilla pair: the carved pumpkin is free
 * and forever and costs the helmet slot; this costs an End farm and lasts a hit's length, the way
 * the turtle shell and the potion are both Water Breathing.
 */
public class AvertedGazeEffect extends MobEffect {
    public AvertedGazeEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }
}
