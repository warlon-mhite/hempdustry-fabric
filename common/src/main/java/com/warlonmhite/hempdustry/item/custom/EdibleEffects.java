package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.EffectLabel;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.platform.Services;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What an edible does: the full design, and the reasoning behind every number, made executable.
 *
 * <h2>Two axes, carried from the cannabutter</h2>
 *
 * <b>Potency</b> (tier I–IV) is <em>how hard</em>; <b>quality</b> is <em>how predictable and how
 * long</em>. Potency comes from the butter's {@code strength} collapsed to four tiers plus a
 * per-edible offset; quality copies across unchanged.
 *
 * <p><b>These are strain-agnostic and always will be.</b> Decarboxylation is where strain identity
 * ends — both strains produce the same {@code decarboxylated_hemp} and cannabutter carries no strain.
 * Do not reach for {@code Strain} anywhere in here.
 *
 * <h2>The bundle is not Purple Kush's and not Lemon Haze's</h2>
 *
 * Absorption rather than Resistance-as-the-spine, specifically so edibles do not obsolete Purple
 * Kush's bong — that still owns the highest Resistance in the mod and delivers it instantly, where an
 * edible trades down to I and pays an onset delay for Absorption, Regeneration and several times the
 * duration. Smoking is fast and mobile; edibles are slow and durable.
 *
 * <h2>The tier buys time, not level (2.0.1)</h2>
 *
 * Before 2.0.1 a Perfect tier-IV brownie was Absorption IV and Resistance II for eight minutes: an
 * enchanted golden apple's Absorption for four times as long, from about one and a half plants. Now
 * Absorption follows the tier only as far as {@code maxBuffLevel} lets it (II by default),
 * Resistance and Regeneration are I at every tier, and the tier scales how long it all lasts. A
 * Perfect batch is still worth the wait: its Slowness never passes I.
 *
 * <h2>Nothing arrives at once</h2>
 *
 * The effects land over a 30-second ramp in the order a real edible presents: the body drop first,
 * then the padded analgesic body, then the munchies, then the restorative peak. <b>Slowness arriving
 * alone and first is the tell</b> — with an onset anywhere from 45 s to 4:20 a player has no
 * other way to know it has started, and feeling heavy is the honest signal rather than a status bar.
 */
public final class EdibleEffects {
    private EdibleEffects() {
    }

    /** Highest potency tier. Four, not more: it keeps stacks merging and fits an effect amplifier. */
    public static final int MAX_TIER = 4;

    /** Cannabutter's {@code strength} cap, mirrored from the Infuser so the quartiles line up. */
    private static final int STRENGTH_CAP = 24;

    // The numbers -- the onset window, the durations, the ramp and the bundle itself -- are data
    // since 2.1: EdibleBundle, whose built-in value carries the reasoning behind each of them.

    /** One effect of the bundle and how many ticks after eating it lands. */
    public record Dose(int delay, MobEffectInstance effect) {
    }

    /** Cannabutter strength (1..24) collapsed to a potency tier (1..4) by even quartiles. */
    public static int tierFromStrength(int strength) {
        int clamped = Mth.clamp(strength, 1, STRENGTH_CAP);
        return Mth.clamp((clamped - 1) * MAX_TIER / STRENGTH_CAP + 1, 1, MAX_TIER);
    }

    /** A tier with an edible's own offset applied. Clamping is what removes any need for a floor. */
    public static int applyOffset(int tier, int offset) {
        return Mth.clamp(tier + offset, 1, MAX_TIER);
    }

    /** The potency an edible stack carries, or 0 if it carries none. */
    public static int potencyOf(ItemStack stack) {
        return stack.getOrDefault(ModComponents.POTENCY, 0);
    }

    /** The quality an edible stack carries, defaulting to the floor. */
    public static Quality qualityOf(ItemStack stack) {
        Quality quality = stack.get(ModComponents.QUALITY);
        return quality == null ? Quality.ROUGH : quality;
    }

    /** How long a tier-IV high of this quality lasts, in ticks, after the server's multiplier. */
    public static int durationTicks(EdibleBundle bundle, Quality quality) {
        return EffectPolicy.duration(bundle.qualities().of(quality).durationTicks());
    }

    /**
     * A random onset delay for this quality, in ticks: one roll of its window, after the server's
     * multiplier. Rough's window is the widest and latest, Perfect's nearly a point.
     */
    public static int rollOnsetTicks(EdibleBundle bundle, Quality quality, RandomGenerator random) {
        EdibleBundle.Onset window = bundle.qualities().of(quality).onset();
        // Scaled by the server's onset multiplier, window and all: a server that wants a 15-second
        // come-up gets a proportionally tighter window rather than a squashed one.
        return EffectPolicy.onset(window.roll(random), window.minTicks(), window.maxTicks());
    }

    /**
     * Queues the whole staggered sequence for {@code player}. Call server-side on eating.
     *
     * <p>Nothing is applied now — the first effect lands after the onset delay, and the rest follow
     * it up the ramp.
     *
     * <p><b>This is the one door every edible goes through</b> — {@link EdibleItem} for the ones you
     * hold and {@code SpaceCakeBlock} for a slice you take off a block — which is why the addons'
     * {@link com.warlonmhite.hempdustry.platform.AddonEvents#afterEat} is fired from here rather than
     * from either call site. An edible added later joins by using the same door.
     */
    public static void consume(Player player, int tier, Quality quality) {
        consume(player, tier, 0, quality);
    }

    /**
     * Eats an edible of THC tier {@code tier} and CBD tier {@code cbdTier} (0 for none of either).
     * The addon event reports the THC tier, 1..4, as it always has, and so fires only when there is
     * one: a CBD-only edible is eaten without it, as its contract says of a tier-0 edible.
     */
    public static void consume(Player player, int tier, int cbdTier, Quality quality) {
        if (tier <= 0 && cbdTier <= 0) {
            // Undosed: nothing is queued, so nothing was consumed in the sense a listener cares
            // about, and no event fires.
            return;
        }
        EdibleBundle data = EdibleBundle.of(player.registryAccess());
        for (Dose dose : bundle(data, tier, cbdTier, quality, rollOnsetTicks(data, quality, ThreadLocalRandom.current()))) {
            queue(player, dose.delay(), dose.effect());
        }

        // Fired on the swallow, not on the onset: the effects above are queued behind a come-up and
        // then ramp in stages, so there is no single later instant that means "this happened".
        if (tier > 0) {
            Services.ADDON_EVENTS.afterEat(player, tier, quality);
        }
    }

    /**
     * The whole staggered sequence for one edible, before the config has had its say, with
     * {@code onset} as the moment it kicks in. Split out of {@link #consume} so the bundle can be read
     * without waiting out a random come-up.
     *
     * <p>Each effect lands its {@code arrives_ticks} after the onset and, unless it has a length of
     * its own per tier, ends with the high rather than outlasting it: the heaviness is what lingers,
     * which is the right way round. A per-tier length is not scaled by the server's duration
     * multiplier, as Regeneration's never was.
     */
    public static List<Dose> bundle(EdibleBundle data, int tier, Quality quality, int onset) {
        return bundle(data, tier, 0, quality, onset);
    }

    /**
     * What an edible of THC tier {@code tier} and CBD tier {@code cbdTier} queues. A THC effect reads
     * the THC tier exactly as before; a CBD effect reads the CBD tier for its length and stays at
     * level I. An effect that answers both takes the longer of its two runs and the higher of its
     * two levels, never their sum: CBD adds no strength to a high and takes none away.
     */
    public static List<Dose> bundle(EdibleBundle data, int tier, int cbdTier, Quality quality, int onset) {
        List<Dose> out = new ArrayList<>();
        for (EdibleBundle.Effect effect : data.effects()) {
            int amplifier = -1;
            int length = 0;
            if (tier > 0 && effect.answers(EffectLabel.THC)) {
                int index = Mth.clamp(tier, 1, MAX_TIER) - 1;
                amplifier = effect.amplifier(index, quality);
                length = lengthAt(data, effect, index, quality);
            }
            if (cbdTier > 0 && effect.answers(EffectLabel.CBD)) {
                int index = Mth.clamp(cbdTier, 1, MAX_TIER) - 1;
                amplifier = Math.max(amplifier, 0);
                length = Math.max(length, lengthAt(data, effect, index, quality));
            }
            if (amplifier >= 0) {
                add(out, onset + effect.arrivesTicks(), effect.effect(), amplifier, length);
            }
        }
        return out;
    }

    private static int lengthAt(EdibleBundle data, EdibleBundle.Effect effect, int index, Quality quality) {
        int duration = (int) (durationTicks(data, quality) * (double) data.tierShare().get(index));
        return effect.durationByTier().map(ticks -> ticks.get(index)).orElse(duration - effect.arrivesTicks());
    }

    /** An edible's CBD tier, 0 for none. */
    public static int cbdPotencyOf(ItemStack stack) {
        return stack.getOrDefault(ModComponents.CBD_POTENCY, 0);
    }

    private static void add(List<Dose> out, int delay, Holder<MobEffect> effect,
                            int amplifier, int duration) {
        if (duration > 0) {
            out.add(new Dose(delay, new MobEffectInstance(effect, duration, amplifier)));
        }
    }

    private static void queue(Player player, int delay, MobEffectInstance effect) {
        // Through the same gate as everything else: an edible obeys effects.enabled, the debuff and
        // munchies switches and the level cap exactly as a bong hit does. Durations arrive already
        // scaled from durationTicks, so the policy's own scaling is not applied twice here -- it is
        // filter() that is wanted, and it returns nothing when the effect is switched off.
        for (MobEffectInstance allowed : EffectPolicy.filterKeepingDuration(List.of(effect))) {
            EdibleScheduler.schedule(player, delay, allowed);
        }
    }

    /**
     * The potency/quality tooltip lines, shared by {@link EdibleItem} and the Space Cake's block item.
     *
     * <p>The same narrow exception cannabutter takes to "you are not wearing a HUD": in-world signals
     * carry the load while the effect runs — the staggered ramp itself tells you it landed — but a
     * stack in your inventory has no in-world surface, and vanilla answers exactly that case with a
     * tooltip line on potions, enchanted books and suspicious stew.
     */
    public static void appendTooltip(ItemStack stack, Consumer<Component> tooltip) {
        int potency = potencyOf(stack);
        int cbd = cbdPotencyOf(stack);
        if (potency <= 0 && cbd <= 0) {
            return;
        }
        tooltip.accept(Chemotype.of(stack).tooltipLine());
        if (potency > 0) {
            tooltip.accept(Component.translatable("hempdustry.edible.potency",
                    Component.translatable("enchantment.level." + potency)).withStyle(ChatFormatting.GRAY));
        }
        if (cbd > 0) {
            tooltip.accept(Component.translatable("hempdustry.edible.cbd_potency",
                    Component.translatable("enchantment.level." + cbd)).withStyle(ChatFormatting.GRAY));
        }
        Quality quality = stack.get(ModComponents.QUALITY);
        if (quality != null) {
            tooltip.accept(Component.translatable("hempdustry.edible.quality",
                    Component.translatable(quality.getTranslationKey())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
