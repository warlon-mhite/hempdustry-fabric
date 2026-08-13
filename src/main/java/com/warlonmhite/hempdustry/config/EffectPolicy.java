package com.warlonmhite.hempdustry.config;

import com.warlonmhite.hempdustry.config.HempdustryConfig.Effects;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

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
 * It scales and it removes; it never adds and never raises. {@code maxLevel} can lower an effect's
 * level but nothing here can push one above what the strain or the edible asked for, so a config
 * cannot invent a Resistance V that the balance was never checked against.
 */
public final class EffectPolicy {
    private EffectPolicy() {
    }

    /**
     * The effects that should actually be applied, after the config has had its say. Returns an empty
     * list when effects are switched off entirely, which every caller already handles because an
     * empty strain does the same thing.
     */
    public static List<StatusEffectInstance> filter(List<StatusEffectInstance> effects) {
        return filter(effects, true);
    }

    /**
     * As {@link #filter}, but leaving the durations alone.
     *
     * <p>For callers that already ran their duration through {@link #duration} — the edibles do,
     * because {@code EdibleEffects.durationTicks} is read for the ramp arithmetic long before the
     * effects are built, and scaling twice would square the multiplier.
     */
    public static List<StatusEffectInstance> filterKeepingDuration(List<StatusEffectInstance> effects) {
        return filter(effects, false);
    }

    private static List<StatusEffectInstance> filter(List<StatusEffectInstance> effects, boolean scaleDuration) {
        Effects config = HempdustryConfig.get().effects();
        if (!config.enabled()) {
            return List.of();
        }
        List<StatusEffectInstance> out = new ArrayList<>(effects.size());
        for (StatusEffectInstance instance : effects) {
            if (!allowed(instance)) {
                continue;
            }
            out.add(new StatusEffectInstance(instance.getEffectType(),
                    scaleDuration ? duration(instance.getDuration()) : instance.getDuration(),
                    amplifier(instance.getAmplifier()),
                    instance.isAmbient(), instance.shouldShowParticles(), instance.shouldShowIcon()));
        }
        return out;
    }

    /** Whether a single effect survives the toggles. */
    private static boolean allowed(StatusEffectInstance instance) {
        Effects config = HempdustryConfig.get().effects();
        if (!config.munchies() && instance.getEffectType() == StatusEffects.HUNGER) {
            return false;
        }
        if (!config.nausea() && instance.getEffectType() == StatusEffects.NAUSEA) {
            return false;
        }
        // Category rather than a list of ids: a datapack strain handing out Wither or Poison is
        // caught by the same switch as the mod's own Mining Fatigue, with no maintenance.
        return config.debuffs() || instance.getEffectType().value().getCategory() != StatusEffectCategory.HARMFUL;
    }

    /** A duration in ticks, scaled. Never rounds to zero — a 1-tick effect is still an effect. */
    public static int duration(int ticks) {
        double scaled = ticks * HempdustryConfig.get().effects().durationMultiplier();
        return Math.max(1, (int) Math.round(scaled));
    }

    /** An amplifier, capped by {@code maxLevel} (which is a level, so level II is amplifier 1). */
    public static int amplifier(int amplifier) {
        return Math.min(amplifier, HempdustryConfig.get().effects().maxLevel() - 1);
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
     * effect it hands out is {@link StatusEffectCategory#HARMFUL} — so with debuffs off,
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
        return Math.max(1, (int) Math.round(oneIn / config.greenOutChanceMultiplier()));
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
        return MathHelper.clamp((int) Math.round(scaled),
                Math.max(1, (int) Math.round(min * HempdustryConfig.get().effects().onsetMultiplier())),
                Math.max(1, (int) Math.round(max * HempdustryConfig.get().effects().onsetMultiplier())));
    }
}
