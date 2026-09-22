package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.api.HempdustryEvents;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What an edible does. See CLAUDE.md §5b D13 for the full design and the reasoning behind every
 * number; this class is the design made executable.
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
 * alone and first is the tell</b> — with an onset anywhere in a 30 s – 3 min window a player has no
 * other way to know it has started, and feeling heavy is the honest signal rather than a status bar.
 */
public final class EdibleEffects {
    private EdibleEffects() {
    }

    /** Highest potency tier. Four, not more: it keeps stacks merging and fits an effect amplifier. */
    public static final int MAX_TIER = 4;

    /** Cannabutter's {@code strength} cap, mirrored from the Infuser so the quartiles line up. */
    private static final int STRENGTH_CAP = 24;

    // ---------------------------------------------------------------------
    // Onset — quality decides how predictable, never how strong
    // ---------------------------------------------------------------------

    /** Earliest and latest an edible can ever kick in, in ticks (30 s and 3 min). */
    private static final int ONSET_MIN = 600;
    private static final int ONSET_MAX = 3600;

    /**
     * Half-widths of each quality's onset window, in ticks, around a common ~90 s centre. Rough is
     * the full spread and Perfect is nearly exact — what a good batch buys is certainty, not power.
     */
    private static final int[] ONSET_SPREAD = {1500, 1050, 600, 60};

    /** The window's centre: 90 s. Every grade is uncertain about the same moment. */
    private static final int ONSET_CENTRE = 1800;

    // ---------------------------------------------------------------------
    // Duration — anchored on vanilla potions, not on a real-world ratio
    // ---------------------------------------------------------------------

    /** Rough is a plain vanilla potion (3:00); Perfect is an extended one (8:00). */
    private static final int[] DURATION = {3600, 5400, 7200, 9600};

    // ---------------------------------------------------------------------
    // The ramp — offsets from the moment it kicks in, not from eating
    // ---------------------------------------------------------------------

    private static final int RAMP_BODY = 160;   // +8s  — absorption and resistance
    private static final int RAMP_HUNGER = 400; // +20s — the munchies genuinely lag
    private static final int RAMP_PEAK = 600;   // +30s — the restorative peak

    /**
     * Each tier's share of the quality's duration, in eighths: 62.5, 75, 87.5 and 100%. This is what
     * a stronger butter buys now that the levels mostly stay put.
     */
    private static final int[] TIER_EIGHTHS = {5, 6, 7, 8};

    /** Slowness per tier, 0-indexed by tier-1 -- except Perfect, whose Slowness never passes I. */
    private static final int[] SLOW_STEP = {0, 0, 1, 1};  // Slowness I, I, II, II
    /** Regeneration I always; only its length grows. 5 s at tier I is vanilla's golden apple. */
    private static final int[] REGEN_DURATION = {100, 200, 300, 400};

    /** The buffs an edible grants, which a full green-out ends along with the smoked ones. */
    public static final List<RegistryEntry<StatusEffect>> BUFFS =
            List.of(StatusEffects.ABSORPTION, StatusEffects.RESISTANCE, StatusEffects.REGENERATION);

    /** One effect of the bundle and how many ticks after eating it lands. */
    public record Dose(int delay, StatusEffectInstance effect) {
    }

    /** Cannabutter strength (1..24) collapsed to a potency tier (1..4) by even quartiles. */
    public static int tierFromStrength(int strength) {
        int clamped = MathHelper.clamp(strength, 1, STRENGTH_CAP);
        return MathHelper.clamp((clamped - 1) * MAX_TIER / STRENGTH_CAP + 1, 1, MAX_TIER);
    }

    /** A tier with an edible's own offset applied. Clamping is what removes any need for a floor. */
    public static int applyOffset(int tier, int offset) {
        return MathHelper.clamp(tier + offset, 1, MAX_TIER);
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

    /** How long the persistent effects last, in ticks. */
    public static int durationTicks(Quality quality) {
        return EffectPolicy.duration(DURATION[quality.ordinal()]);
    }

    /**
     * A random onset delay for this quality, in ticks. Rough can land anywhere in the full window;
     * Perfect is near-exact. All four are uncertain about the same ~90 s centre.
     */
    public static int rollOnsetTicks(Quality quality) {
        int spread = ONSET_SPREAD[quality.ordinal()];
        int roll = ONSET_CENTRE + ThreadLocalRandom.current().nextInt(-spread, spread + 1);
        // Scaled by the server's onset multiplier, window and all: a server that wants a 15-second
        // come-up gets a proportionally tighter spread rather than a squashed one.
        return EffectPolicy.onset(MathHelper.clamp(roll, ONSET_MIN, ONSET_MAX), ONSET_MIN, ONSET_MAX);
    }

    /**
     * Queues the whole staggered sequence for {@code player}. Call server-side on eating.
     *
     * <p>Nothing is applied now — the first effect lands after the onset delay, and the rest follow
     * it up the ramp.
     *
     * <p><b>This is the one door every edible goes through</b> — {@link EdibleItem} for the ones you
     * hold and {@code SpaceCakeBlock} for a slice you take off a block — which is why
     * {@link HempdustryEvents#AFTER_EAT} is fired from here rather than from either call site. An
     * edible added later joins by using the same door.
     */
    public static void consume(PlayerEntity player, int tier, Quality quality) {
        if (tier <= 0) {
            // Undosed: nothing is queued, so nothing was consumed in the sense a listener cares
            // about, and no event fires.
            return;
        }
        for (Dose dose : bundle(tier, quality, rollOnsetTicks(quality))) {
            queue(player, dose.delay(), dose.effect());
        }

        // Fired on the swallow, not on the onset: the effects above are queued behind a come-up and
        // then ramp in stages, so there is no single later instant that means "this happened".
        HempdustryEvents.AFTER_EAT.invoker().onEaten(player, tier, quality);
    }

    /**
     * The whole staggered sequence for one edible, before the config has had its say, with
     * {@code onset} as the moment it kicks in. Split out of {@link #consume} so the bundle can be read
     * without waiting out a random come-up.
     */
    public static List<Dose> bundle(int tier, Quality quality, int onset) {
        int index = MathHelper.clamp(tier, 1, MAX_TIER) - 1;
        int duration = durationTicks(quality) * TIER_EIGHTHS[index] / 8;
        int slowness = quality == Quality.PERFECT ? 0 : SLOW_STEP[index];
        List<Dose> out = new ArrayList<>();

        // The body drop, alone and first. This is what tells the player it has started.
        add(out, onset, StatusEffects.SLOWNESS, slowness, duration);

        // The padded, pain-dulled body. Ends with the slowness rather than outlasting it, so the
        // heaviness is what lingers -- which is the right way round. Absorption asks for the tier
        // and EffectPolicy stops it at maxBuffLevel.
        add(out, onset + RAMP_BODY, StatusEffects.ABSORPTION, index, duration - RAMP_BODY);
        add(out, onset + RAMP_BODY, StatusEffects.RESISTANCE, 0, duration - RAMP_BODY);

        // Munchies, which genuinely arrive later than the rest, and stay to the end: a minute of
        // Hunger I was a point and a half of saturation, a cost nobody could see.
        add(out, onset + RAMP_HUNGER, StatusEffects.HUNGER, 0, duration - RAMP_HUNGER);

        // The restorative peak, last.
        add(out, onset + RAMP_PEAK, StatusEffects.REGENERATION, 0, REGEN_DURATION[index]);
        return out;
    }

    private static void add(List<Dose> out, int delay, RegistryEntry<StatusEffect> effect,
                            int amplifier, int duration) {
        if (duration > 0) {
            out.add(new Dose(delay, new StatusEffectInstance(effect, duration, amplifier)));
        }
    }

    private static void queue(PlayerEntity player, int delay, StatusEffectInstance effect) {
        // Through the same gate as everything else: an edible obeys effects.enabled, the debuff and
        // munchies switches and the level cap exactly as a bong hit does. Durations arrive already
        // scaled from durationTicks, so the policy's own scaling is not applied twice here -- it is
        // filter() that is wanted, and it returns nothing when the effect is switched off.
        for (StatusEffectInstance allowed : EffectPolicy.filterKeepingDuration(List.of(effect))) {
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
    public static void appendTooltip(ItemStack stack, List<Text> tooltip) {
        int potency = potencyOf(stack);
        if (potency <= 0) {
            return;
        }
        tooltip.add(Text.translatable("hempdustry.edible.potency",
                Text.translatable("enchantment.level." + potency)).formatted(Formatting.GRAY));
        Quality quality = stack.get(ModComponents.QUALITY);
        if (quality != null) {
            tooltip.add(Text.translatable("hempdustry.edible.quality",
                    Text.translatable(quality.getTranslationKey())).formatted(Formatting.DARK_GRAY));
        }
    }
}
