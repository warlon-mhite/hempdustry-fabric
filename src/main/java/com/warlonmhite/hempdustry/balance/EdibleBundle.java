package com.warlonmhite.hempdustry.balance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * What an edible does, as data: {@code data/<namespace>/hempdustry/edible/default.json}. One bundle
 * for every edible, because edibles are strain-agnostic — decarboxylation is where a strain's
 * identity ends, and the cannabutter carries none.
 *
 * <p>Every per-tier list has exactly four entries, tier I first. The tier is an effect amplifier and
 * a block state on the Space Cake, so it is the one thing here a datapack cannot widen.
 * {@link EdibleEffects} turns this into effects, and keeps the reasoning behind the built-in numbers.
 */
public record EdibleBundle(Qualities qualities, List<Float> tierShare, List<Effect> effects) {

    /**
     * When an edible of one quality kicks in: never before {@code min}, never after {@code max}, and
     * most often at {@code mode}. The roll is a triangle over the three, so every tick of the window
     * can come up and the odds thin out evenly towards both ends -- a mode well short of {@code max}
     * is the long late tail an edible is known for.
     */
    public record Onset(int minTicks, int modeTicks, int maxTicks) {
        public static final Codec<Onset> CODEC = RecordCodecBuilder.<Onset>create(instance -> instance.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("min").forGetter(Onset::minTicks),
                ExtraCodecs.POSITIVE_INT.fieldOf("mode").forGetter(Onset::modeTicks),
                ExtraCodecs.POSITIVE_INT.fieldOf("max").forGetter(Onset::maxTicks)
        ).apply(instance, Onset::new)).validate(onset -> onset.minTicks() <= onset.modeTicks()
                && onset.modeTicks() <= onset.maxTicks()
                ? DataResult.success(onset)
                : DataResult.error(() -> "onset_ticks wants min <= mode <= max"));

        /** One roll of the triangle, in ticks. */
        public int roll(RandomGenerator random) {
            if (maxTicks == minTicks) {
                return minTicks;
            }
            double width = maxTicks - minTicks;
            double u = random.nextDouble();
            double rising = (modeTicks - minTicks) / width;
            double ticks = u < rising
                    ? minTicks + Math.sqrt(u * width * (modeTicks - minTicks))
                    : maxTicks - Math.sqrt((1 - u) * width * (maxTicks - modeTicks));
            return Math.clamp(Math.round(ticks), minTicks, maxTicks);
        }
    }

    /** One quality: when it kicks in, and how long a tier-IV high lasts. */
    public record Grade(Onset onset, int durationTicks) {
        public static final Codec<Grade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Onset.CODEC.fieldOf("onset_ticks").forGetter(Grade::onset),
                ExtraCodecs.POSITIVE_INT.fieldOf("duration_ticks").forGetter(Grade::durationTicks)
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
    public record Effect(Holder<MobEffect> effect, List<Integer> amplifierByTier, int arrivesTicks,
                         Optional<List<Integer>> durationByTier, Map<Quality, List<Integer>> amplifierByQuality) {
        private static final Codec<List<Integer>> AMPLIFIERS =
                Codec.intRange(0, 255).listOf(EdibleEffects.MAX_TIER, EdibleEffects.MAX_TIER);
        private static final Codec<List<Integer>> TICKS =
                ExtraCodecs.POSITIVE_INT.listOf(EdibleEffects.MAX_TIER, EdibleEffects.MAX_TIER);

        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                MobEffect.CODEC.fieldOf("effect").forGetter(Effect::effect),
                AMPLIFIERS.fieldOf("amplifier_by_tier").forGetter(Effect::amplifierByTier),
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("arrives_ticks").forGetter(Effect::arrivesTicks),
                TICKS.optionalFieldOf("duration_by_tier").forGetter(Effect::durationByTier),
                Codec.unboundedMap(Quality.CODEC, AMPLIFIERS).optionalFieldOf("amplifier_by_quality", Map.of())
                        .forGetter(Effect::amplifierByQuality)
        ).apply(instance, Effect::new));

        /** The amplifier at {@code tierIndex} (tier I is 0) for an edible of {@code quality}. */
        public int amplifier(int tierIndex, Quality quality) {
            return amplifierByQuality.getOrDefault(quality, amplifierByTier).get(tierIndex);
        }
    }

    public static final ResourceKey<Registry<EdibleBundle>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "edible"));

    public static final ResourceKey<EdibleBundle> DEFAULT =
            ResourceKey.create(REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "default"));

    public static final Codec<EdibleBundle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Qualities.CODEC.fieldOf("qualities").forGetter(EdibleBundle::qualities),
            ExtraCodecs.POSITIVE_FLOAT.listOf(EdibleEffects.MAX_TIER, EdibleEffects.MAX_TIER)
                    .fieldOf("tier_share").forGetter(EdibleBundle::tierShare),
            Effect.CODEC.listOf().fieldOf("effects").forGetter(EdibleBundle::effects)
    ).apply(instance, EdibleBundle::new));

    /**
     * The built-in bundle: 2.0.1's effects and durations, 2.1's onsets.
     *
     * <ul>
     *   <li><b>Onset</b> (2.1): the worse the batch, the wider the window and the later it most
     *       likely lands. Perfect is 87 to 93 s, around 90; Clean 1:05 to 2:15, most likely 1:30;
     *       Standard 0:55 to 3:15, most likely 1:40; Rough 0:45 to 4:20, most likely 2:00 -- it can
     *       catch you early or keep you waiting, which is the edible's own trap. A good batch buys
     *       certainty and an earlier come-up, never a stronger one. Until 2.1 every quality shared
     *       one centre and a clamp, which piled a tenth of Rough's rolls onto the window's first
     *       tick and never reached its last fifteen seconds.</li>
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
            new Qualities(new Grade(new Onset(900, 2400, 5200), 3600), new Grade(new Onset(1100, 2000, 3900), 5400),
                    new Grade(new Onset(1300, 1800, 2700), 7200), new Grade(new Onset(1740, 1800, 1860), 9600)),
            List.of(0.625F, 0.75F, 0.875F, 1.0F),
            List.of(
                    new Effect(MobEffects.MOVEMENT_SLOWDOWN, List.of(0, 0, 1, 1), 0, Optional.empty(),
                            Map.of(Quality.PERFECT, List.of(0, 0, 0, 0))),
                    new Effect(MobEffects.ABSORPTION, List.of(0, 1, 2, 3), 160, Optional.empty(), Map.of()),
                    new Effect(MobEffects.DAMAGE_RESISTANCE, List.of(0, 0, 0, 0), 160, Optional.empty(), Map.of()),
                    new Effect(MobEffects.HUNGER, List.of(0, 0, 0, 0), 400, Optional.empty(), Map.of()),
                    new Effect(MobEffects.REGENERATION, List.of(0, 0, 0, 0), 600,
                            Optional.of(List.of(100, 200, 300, 400)), Map.of())));

    public static void bootstrap(BootstrapContext<EdibleBundle> registerable) {
        registerable.register(DEFAULT, BUILT_IN);
    }

    /** The bundle every edible gives in this world. */
    public static EdibleBundle of(HolderLookup.Provider registries) {
        return ModBalance.entry(registries, DEFAULT, BUILT_IN);
    }
}
