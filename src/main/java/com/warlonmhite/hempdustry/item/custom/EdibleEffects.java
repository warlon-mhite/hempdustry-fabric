package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

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
 * Do not reach for {@link Strain} anywhere in here.
 *
 * <h2>The bundle is not Purple Kush's and not Lemon Haze's</h2>
 *
 * Absorption rather than Resistance-as-the-spine, specifically so edibles do not obsolete Purple
 * Kush's bong — that still owns the highest Resistance in the mod (III) and delivers it instantly,
 * where an edible trades down to II and pays an onset delay for Absorption, Regeneration and several
 * times the duration. Smoking is fast and mobile; edibles are slow and durable.
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

    /** Munchies run 60 s flat whatever the tier — vanilla's own Hunger tops out at 30 s. */
    private static final int HUNGER_DURATION = 1200;

    /** Amplifier per tier for the two effects that scale slowly. 0-indexed by tier-1. */
    private static final int[] SLOW_STEP = {0, 0, 1, 1};  // Slowness I, I, II, II
    private static final int[] RESIST_STEP = {0, 0, 1, 1}; // Resistance I, I, II, II
    /** Regeneration II always; only its length grows. 5 s at tier I is vanilla's golden apple. */
    private static final int[] REGEN_DURATION = {100, 200, 300, 400};

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
        return DURATION[quality.ordinal()];
    }

    /**
     * A random onset delay for this quality, in ticks. Rough can land anywhere in the full window;
     * Perfect is near-exact. All four are uncertain about the same ~90 s centre.
     */
    public static int rollOnsetTicks(Quality quality) {
        int spread = ONSET_SPREAD[quality.ordinal()];
        int roll = ONSET_CENTRE + ThreadLocalRandom.current().nextInt(-spread, spread + 1);
        return MathHelper.clamp(roll, ONSET_MIN, ONSET_MAX);
    }

    /**
     * Queues the whole staggered sequence for {@code player}. Call server-side on eating.
     *
     * <p>Nothing is applied now — the first effect lands after the onset delay, and the rest follow
     * it up the ramp.
     */
    public static void consume(ServerWorld world, PlayerEntity player, int tier, Quality quality) {
        if (tier <= 0) {
            return;
        }
        int onset = rollOnsetTicks(quality);
        int duration = durationTicks(quality);
        int index = MathHelper.clamp(tier, 1, MAX_TIER) - 1;

        // The body drop, alone and first. This is what tells the player it has started.
        queue(world, player, onset, StatusEffects.SLOWNESS, SLOW_STEP[index], duration);

        // The padded, pain-dulled body. Ends with the slowness rather than outlasting it, so the
        // heaviness is what lingers -- which is the right way round.
        queue(world, player, onset + RAMP_BODY, StatusEffects.ABSORPTION, index, duration - RAMP_BODY);
        queue(world, player, onset + RAMP_BODY, StatusEffects.RESISTANCE, RESIST_STEP[index], duration - RAMP_BODY);

        // Munchies, which genuinely arrive later than the rest.
        queue(world, player, onset + RAMP_HUNGER, StatusEffects.HUNGER, 0, HUNGER_DURATION);

        // The restorative peak, last.
        queue(world, player, onset + RAMP_PEAK, StatusEffects.REGENERATION, 1, REGEN_DURATION[index]);
    }

    private static void queue(ServerWorld world, PlayerEntity player, int delay,
                              RegistryEntry<StatusEffect> effect, int amplifier, int duration) {
        if (duration <= 0) {
            return;
        }
        EdibleScheduler.schedule(world, player, delay, new StatusEffectInstance(effect, duration, amplifier));
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

    /** Every effect an edible of this tier/quality will eventually apply, for the tooltip. */
    public static List<StatusEffectInstance> preview(int tier, Quality quality) {
        int index = MathHelper.clamp(tier, 1, MAX_TIER) - 1;
        int duration = durationTicks(quality);
        return List.of(
                new StatusEffectInstance(StatusEffects.ABSORPTION, duration - RAMP_BODY, index),
                new StatusEffectInstance(StatusEffects.RESISTANCE, duration - RAMP_BODY, RESIST_STEP[index]),
                new StatusEffectInstance(StatusEffects.REGENERATION, REGEN_DURATION[index], 1),
                new StatusEffectInstance(StatusEffects.SLOWNESS, duration, SLOW_STEP[index]),
                new StatusEffectInstance(StatusEffects.HUNGER, HUNGER_DURATION, 0));
    }
}
