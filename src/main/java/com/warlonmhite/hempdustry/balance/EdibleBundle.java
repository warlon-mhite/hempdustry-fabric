package com.warlonmhite.hempdustry.balance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What an edible does, as data: {@code data/<namespace>/hempdustry/edible/default.json}. One bundle
 * for every edible, because edibles are strain-agnostic — decarboxylation is where a strain's
 * identity ends, and the cannabutter carries none.
 *
 * <p>Every per-tier list has exactly four entries, tier I first. The tier is an effect amplifier and
 * a block state on the Space Cake, so it is the one thing here a datapack cannot widen.
 * {@link EdibleEffects} turns this into effects, and keeps the reasoning behind the built-in numbers.
 */
public record EdibleBundle(Onset onset, Qualities qualities, List<Float> tierShare, List<Effect> effects) {

    /** When an edible kicks in: around {@code centre}, never before {@code earliest} nor after {@code latest}. */
    public record Onset(int earliestTicks, int latestTicks, int centreTicks) {
        public static final Codec<Onset> CODEC = RecordCodecBuilder.<Onset>create(instance -> instance.group(
                Codecs.POSITIVE_INT.fieldOf("earliest_ticks").forGetter(Onset::earliestTicks),
                Codecs.POSITIVE_INT.fieldOf("latest_ticks").forGetter(Onset::latestTicks),
                Codecs.POSITIVE_INT.fieldOf("centre_ticks").forGetter(Onset::centreTicks)
        ).apply(instance, Onset::new)).validate(onset -> onset.earliestTicks() <= onset.latestTicks()
                ? DataResult.success(onset)
                : DataResult.error(() -> "earliest_ticks is after latest_ticks"));
    }

    /** One quality's come-up, as a spread either side of the centre, and how long a tier-IV high lasts. */
    public record Grade(int onsetSpreadTicks, int durationTicks) {
        public static final Codec<Grade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codecs.NON_NEGATIVE_INT.fieldOf("onset_spread_ticks").forGetter(Grade::onsetSpreadTicks),
                Codecs.POSITIVE_INT.fieldOf("duration_ticks").forGetter(Grade::durationTicks)
        ).apply(instance, Grade::new));
    }

    /** All four qualities, each written out: an edible of any quality has to know what it does. */
    public record Qualities(Grade rough, Grade standard, Grade clean, Grade perfect) {
        public static final Codec<Qualities> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Grade.CODEC.fieldOf("rough").forGetter(Qualities::rough),
                Grade.CODEC.fieldOf("standard").forGetter(Qualities::standard),
                Grade.CODEC.fieldOf("clean").forGetter(Qualities::clean),
                Grade.CODEC.fieldOf("perfect").forGetter(Qualities::perfect)
        ).apply(instance, Qualities::new));

        public Grade of(Quality quality) {
            return switch (quality) {
                case ROUGH -> rough;
                case STANDARD -> standard;
                case CLEAN -> clean;
                case PERFECT -> perfect;
            };
        }
    }

    /**
     * One effect of the bundle. It lands {@code arrives_ticks} after the edible kicks in and lasts to
     * the end of the high, so everything ends together, unless it has a length of its own per tier.
     * {@code amplifier_by_quality} replaces the per-tier amplifiers for the qualities it names.
     */
    public record Effect(RegistryEntry<StatusEffect> effect, List<Integer> amplifierByTier, int arrivesTicks,
                         Optional<List<Integer>> durationByTier, Map<Quality, List<Integer>> amplifierByQuality) {
        private static final Codec<List<Integer>> AMPLIFIERS =
                Codec.intRange(0, 255).listOf(EdibleEffects.MAX_TIER, EdibleEffects.MAX_TIER);
        private static final Codec<List<Integer>> TICKS =
                Codecs.POSITIVE_INT.listOf(EdibleEffects.MAX_TIER, EdibleEffects.MAX_TIER);

        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StatusEffect.ENTRY_CODEC.fieldOf("effect").forGetter(Effect::effect),
                AMPLIFIERS.fieldOf("amplifier_by_tier").forGetter(Effect::amplifierByTier),
                Codecs.NON_NEGATIVE_INT.fieldOf("arrives_ticks").forGetter(Effect::arrivesTicks),
                TICKS.optionalFieldOf("duration_by_tier").forGetter(Effect::durationByTier),
                Codec.unboundedMap(Quality.CODEC, AMPLIFIERS).optionalFieldOf("amplifier_by_quality", Map.of())
                        .forGetter(Effect::amplifierByQuality)
        ).apply(instance, Effect::new));

        /** The amplifier at {@code tierIndex} (tier I is 0) for an edible of {@code quality}. */
        public int amplifier(int tierIndex, Quality quality) {
            return amplifierByQuality.getOrDefault(quality, amplifierByTier).get(tierIndex);
        }
    }

    public static final RegistryKey<Registry<EdibleBundle>> REGISTRY_KEY =
            RegistryKey.ofRegistry(Identifier.of(Hempdustry.MOD_ID, "edible"));

    public static final RegistryKey<EdibleBundle> DEFAULT =
            RegistryKey.of(REGISTRY_KEY, Identifier.of(Hempdustry.MOD_ID, "default"));

    public static final Codec<EdibleBundle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Onset.CODEC.fieldOf("onset").forGetter(EdibleBundle::onset),
            Qualities.CODEC.fieldOf("qualities").forGetter(EdibleBundle::qualities),
            Codecs.POSITIVE_FLOAT.listOf(EdibleEffects.MAX_TIER, EdibleEffects.MAX_TIER)
                    .fieldOf("tier_share").forGetter(EdibleBundle::tierShare),
            Effect.CODEC.listOf().fieldOf("effects").forGetter(EdibleBundle::effects)
    ).apply(instance, EdibleBundle::new));

    /**
     * The 2.0.1 bundle.
     *
     * <ul>
     *   <li><b>Onset</b> anywhere from 30 s to 3 min, every quality uncertain about the same ~90 s
     *       centre. What narrows with quality is the spread: Rough is the whole window, Perfect is
     *       nearly exact. A good batch buys certainty, not power.</li>
     *   <li><b>Duration</b> is anchored on vanilla's potions, not on a real-world ratio: Rough lasts a
     *       plain potion's 3:00, Perfect an extended one's 8:00. Tiers I to IV take 62.5, 75, 87.5
     *       and 100% of it — what a stronger butter buys, now that the levels mostly stay put.</li>
     *   <li><b>The ramp</b>, from the moment it kicks in: Slowness alone and first, the tell that it
     *       has started; Absorption and Resistance at +8 s; the munchies at +20 s, to the end;
     *       Regeneration I last, at +30 s, for 5 to 20 s by tier (5 s is the golden apple's).
     *       Perfect butter's Slowness never passes I. Absorption asks for the tier and the config's
     *       {@code maxBuffLevel} stops it, at II by default.</li>
     * </ul>
     */
    public static final EdibleBundle BUILT_IN = new EdibleBundle(
            new Onset(600, 3600, 1800),
            new Qualities(new Grade(1500, 3600), new Grade(1050, 5400), new Grade(600, 7200), new Grade(60, 9600)),
            List.of(0.625F, 0.75F, 0.875F, 1.0F),
            List.of(
                    new Effect(StatusEffects.SLOWNESS, List.of(0, 0, 1, 1), 0, Optional.empty(),
                            Map.of(Quality.PERFECT, List.of(0, 0, 0, 0))),
                    new Effect(StatusEffects.ABSORPTION, List.of(0, 1, 2, 3), 160, Optional.empty(), Map.of()),
                    new Effect(StatusEffects.RESISTANCE, List.of(0, 0, 0, 0), 160, Optional.empty(), Map.of()),
                    new Effect(StatusEffects.HUNGER, List.of(0, 0, 0, 0), 400, Optional.empty(), Map.of()),
                    new Effect(StatusEffects.REGENERATION, List.of(0, 0, 0, 0), 600,
                            Optional.of(List.of(100, 200, 300, 400)), Map.of())));

    public static void bootstrap(Registerable<EdibleBundle> registerable) {
        registerable.register(DEFAULT, BUILT_IN);
    }

    /** The bundle every edible gives in this world. */
    public static EdibleBundle of(RegistryWrapper.WrapperLookup registries) {
        return ModBalance.entry(registries, DEFAULT, BUILT_IN);
    }
}
