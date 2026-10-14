package com.warlonmhite.hempdustry.config;

import com.warlonmhite.hempdustry.config.HempdustryConfig.Effects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * The single gate every status effect this mod applies passes through.
 *
 * <p>It exists so the config's effect knobs are implemented <b>once</b> rather than at each of the
 * half-dozen places that hand out effects — a smoking hit, a green-out, an edible's staggered ramp,
 * the nausea rolls. A new source of effects gets the config for free by calling {@link #filter}, and
 * <b>forgetting to call it is the only way to bypass the admin's settings</b>, which is a much easier
 * mistake to spot in review than a missing multiplier.
 *
 * <h2>What it does not do</h2>
 *
 * It scales and it removes; it never adds and never raises. {@code maxLevel} and {@code maxBuffLevel}
 * can lower an effect's level but nothing here can push one above what the strain or the edible asked
 * for, so a config cannot invent a Resistance V that the balance was never checked against.
 */
public final class EffectPolicy {
    private EffectPolicy() {
    }

    /**
     * The effects that should actually be applied, after the config has had its say. Returns an empty
     * list when effects are switched off entirely, which every caller already handles because an
     * empty strain does the same thing.
     */
    public static List<MobEffectInstance> filter(List<MobEffectInstance> effects) {
        return filter(effects, true, 0);
    }

    /**
     * As {@link #filter}, for a smoked load that may pass the buff cap by {@code buffBonus} levels:
     * one when a bud is loaded with a concentrate, else none. The load has already held every entry
     * that did not earn the bonus to the plain cap, so all this does is not clip the one that did.
     */
    public static List<MobEffectInstance> filter(List<MobEffectInstance> effects, int buffBonus) {
        return filter(effects, true, buffBonus);
    }

    /**
     * As {@link #filter}, but leaving the durations alone.
     *
     * <p>For callers that already ran their duration through {@link #duration} — the edibles do,
     * because {@code EdibleEffects.durationTicks} is read for the ramp arithmetic long before the
     * effects are built, and scaling twice would square the multiplier.
     */
    public static List<MobEffectInstance> filterKeepingDuration(List<MobEffectInstance> effects) {
        return filter(effects, false, 0);
    }

    private static List<MobEffectInstance> filter(List<MobEffectInstance> effects, boolean scaleDuration,
                                                     int buffBonus) {
        Effects config = HempdustryConfig.get().effects();
        if (!config.enabled()) {
            return List.of();
        }
        List<MobEffectInstance> out = new ArrayList<>(effects.size());
        for (MobEffectInstance instance : effects) {
            if (!allowed(instance)) {
                continue;
            }
            out.add(new MobEffectInstance(instance.getEffect(),
                    scaleDuration ? duration(instance.getDuration()) : instance.getDuration(),
                    amplifier(instance.getEffect(), instance.getAmplifier(), buffBonus),
                    instance.isAmbient(), instance.isVisible(), instance.showIcon()));
        }
        return out;
    }

    /** Whether a single effect survives the toggles. */
    private static boolean allowed(MobEffectInstance instance) {
        Effects config = HempdustryConfig.get().effects();
        if (!config.munchies() && instance.getEffect() == MobEffects.HUNGER) {
            return false;
        }
        if (!config.nausea() && instance.getEffect() == MobEffects.NAUSEA) {
            return false;
        }
        // Category rather than a list of ids: a datapack strain handing out Wither or Poison is
        // caught by the same switch as the mod's own Mining Fatigue, with no maintenance.
        return config.debuffs() || instance.getEffect().value().getCategory() != MobEffectCategory.HARMFUL;
    }

    /** A duration in ticks, scaled. Never rounds to zero — a 1-tick effect is still an effect. */
    public static int duration(int ticks) {
        double scaled = ticks * HempdustryConfig.get().effects().durationMultiplier();
        return Math.max(1, (int) Math.round(scaled));
    }

    /**
     * An amplifier, capped by {@code maxLevel} and, for a {@link MobEffectCategory#BENEFICIAL}
     * effect, by {@code maxBuffLevel} too. Both are levels, so level II is amplifier 1.
     *
     * <p>By category rather than by a list, for the same reason {@link #allowed} is: a datapack
     * strain's Regeneration is a buff the day it is written. A cost is left to scale with the dose,
     * which is what makes a big dose a real decision now that its buffs stop early.
     */
    public static int amplifier(Holder<MobEffect> effect, int amplifier) {
        return amplifier(effect, amplifier, 0);
    }

    /** As above, with the buff cap raised by {@code buffBonus}; {@code maxLevel} still holds. */
    public static int amplifier(Holder<MobEffect> effect, int amplifier, int buffBonus) {
        Effects config = HempdustryConfig.get().effects();
        int cap = config.maxLevel();
        if (effect.value().getCategory() == MobEffectCategory.BENEFICIAL) {
            cap = Math.min(cap, config.maxBuffLevel() + buffBonus);
        }
        return Math.min(amplifier, cap - 1);
    }

    /**
     * The highest level a buff reaches, which is also where extra dose turns into duration. That is
     * {@code maxBuffLevel}, raised by {@code buffBonus} for a bud loaded with a concentrate, or
     * {@code maxLevel} where it is the lower of the two: under {@code maxLevel: 1} a dose of two is
     * level I, paired or not, and its second bud has to buy time or it buys nothing at all.
     */
    public static int maxBuffLevel(int buffBonus) {
        Effects config = HempdustryConfig.get().effects();
        return Math.min(config.maxLevel(), config.maxBuffLevel() + buffBonus);
    }

    /** A use cooldown in ticks, scaled. Zero is allowed here: it means "no cooldown". */
    public static int cooldown(int ticks) {
        double scaled = ticks * HempdustryConfig.get().effects().cooldownMultiplier();
        return Math.max(0, (int) Math.round(scaled));
    }

    /**
     * A "one in N" green-out chance, adjusted. Returns {@code 0} for "never", which is what the
     * callers already use for a dose that cannot green out at all.
     *
     * <p>The multiplier raises the <em>chance</em>, so it divides N: 2.0 doubles the odds. Turning
     * green-outs off, or a multiplier of 0, gives never.
     *
     * <p><b>{@code debuffs = false} also gives never</b>, and that is not the same knob being read
     * twice. A green-out <em>replaces</em> the hit's effects rather than layering on them, and every
     * effect it hands out is {@link MobEffectCategory#HARMFUL} — so with debuffs off,
     * {@link #filter} would strip all four and the roll would resolve into <em>nothing at all</em>:
     * the buffs skipped, no penalty applied, no sound, no explanation for the three buds. Deciding
     * it here rather than at the roll site is what stops that, because this is the one place that
     * knows a green-out is all debuff.
     */
    public static int greenOutChanceOneIn(int oneIn) {
        Effects config = HempdustryConfig.get().effects();
        if (oneIn <= 0 || !config.greenOut() || !config.debuffs()
                || config.greenOutChanceMultiplier() <= 0) {
            return 0;
        }
        // Capped before the cast: a multiplier just above 0 makes the odds astronomical, and a long
        // that big narrowed to an int wraps — often negative, which the max(1, …) then turned into
        // a green-out on every hit.
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                Math.round(oneIn / config.greenOutChanceMultiplier())));
    }

    /**
     * A "one in N" chance for the cough and nausea rolls. Nausea being switched off is handled here
     * rather than at the roll site so the caller cannot forget it.
     */
    public static int nauseaChanceOneIn(int oneIn) {
        return HempdustryConfig.get().effects().nausea() ? oneIn : 0;
    }

    /** An edible's onset delay, scaled and kept inside the window the design guarantees. */
    public static int onset(int ticks, int min, int max) {
        double scaled = ticks * HempdustryConfig.get().effects().onsetMultiplier();
        return Mth.clamp((int) Math.round(scaled),
                Math.max(1, (int) Math.round(min * HempdustryConfig.get().effects().onsetMultiplier())),
                Math.max(1, (int) Math.round(max * HempdustryConfig.get().effects().onsetMultiplier())));
    }
}
