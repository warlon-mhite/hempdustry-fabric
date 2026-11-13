package com.warlonmhite.hempdustry.strain;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.RandomSource;

/**
 * A strain's odds for one {@link StressPhenomenon}: the weight of each {@link Resistance} step a plant of it
 * rolls at planting, very weak first. The weights need not add up to 100.
 *
 * <p>Most strains name only their most likely step, and the odds come from one shared spread ({@link #spread}):
 * 60 on the step, 25 one step away, 15 two away, each share split between both sides. Away from the ends, a
 * half that would fall off the scale lands on its end step; at an end step, the other side takes it all.
 *
 * <table>
 *   <tr><th>most likely</th><th>VW / W / N / R / VR</th></tr>
 *   <tr><td>very weak</td><td>60 / 25 / 15 / 0 / 0</td></tr>
 *   <tr><td>weak</td><td>19 / 62 / 12 / 7 / 0</td></tr>
 *   <tr><td>neutral</td><td>7 / 12 / 62 / 12 / 7</td></tr>
 *   <tr><td>resistant</td><td>0 / 7 / 12 / 62 / 19</td></tr>
 *   <tr><td>very resistant</td><td>0 / 0 / 15 / 25 / 60</td></tr>
 * </table>
 *
 * <p>Written in a strain's file either as that step, {@code "light": "resistant"}, or as five weights,
 * {@code "light": {"very_weak": 0, "weak": 5, "neutral": 20, "resistant": 60, "very_resistant": 15}} (a weight
 * left out is 0). Odds equal to a step's spread are written as the step.
 */
public record ResistanceOdds(int veryWeak, int weak, int neutral, int resistant, int veryResistant) {

    /** Every plant neutral: a phenomenon a strain file does not name, as every plant was before resistances. */
    public static final ResistanceOdds ALWAYS_NEUTRAL = new ResistanceOdds(0, 0, 100, 0, 0);

    /** The shared spread's shares: on the step, one step away, two steps away. */
    private static final int[] SPREAD = {60, 25, 15};

    private static final Codec<ResistanceOdds> WEIGHTS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 1000).optionalFieldOf("very_weak", 0).forGetter(ResistanceOdds::veryWeak),
            Codec.intRange(0, 1000).optionalFieldOf("weak", 0).forGetter(ResistanceOdds::weak),
            Codec.intRange(0, 1000).optionalFieldOf("neutral", 0).forGetter(ResistanceOdds::neutral),
            Codec.intRange(0, 1000).optionalFieldOf("resistant", 0).forGetter(ResistanceOdds::resistant),
            Codec.intRange(0, 1000).optionalFieldOf("very_resistant", 0).forGetter(ResistanceOdds::veryResistant)
    ).apply(instance, ResistanceOdds::new));

    public static final Codec<ResistanceOdds> CODEC = Codec.either(Resistance.CODEC, WEIGHTS_CODEC).xmap(
            either -> either.map(ResistanceOdds::spread, odds -> odds),
            odds -> odds.mostLikelyIfSpread().<Either<Resistance, ResistanceOdds>>map(Either::left)
                    .orElseGet(() -> Either.right(odds)));

    /** The odds a strain whose most likely step is {@code mostLikely} rolls from, by the shared spread. */
    public static ResistanceOdds spread(Resistance mostLikely) {
        int centre = mostLikely.ordinal();
        int last = Resistance.values().length - 1;
        boolean end = centre == 0 || centre == last;
        int[] weights = new int[last + 1];
        for (int away = 1; away < SPREAD.length; away++) {
            int below = centre - away;
            int above = centre + away;
            if (end) {
                // At an end step the other side takes the whole share.
                weights[below >= 0 ? below : above] += SPREAD[away];
            } else {
                // Each side takes half; a half that falls off the scale lands on its end step.
                int half = SPREAD[away] / 2;
                weights[Math.max(0, below)] += half;
                weights[Math.min(last, above)] += half;
            }
        }
        int rest = 100;
        for (int weight : weights) {
            rest -= weight;
        }
        weights[centre] += rest;
        return new ResistanceOdds(weights[0], weights[1], weights[2], weights[3], weights[4]);
    }

    /** The weight of {@code step} among the five. */
    public int weight(Resistance step) {
        return switch (step) {
            case VERY_WEAK -> veryWeak;
            case WEAK -> weak;
            case NEUTRAL -> neutral;
            case RESISTANT -> resistant;
            case VERY_RESISTANT -> veryResistant;
        };
    }

    /** The step whose spread these odds are, if they are one. */
    public Optional<Resistance> mostLikelyIfSpread() {
        for (Resistance step : Resistance.values()) {
            if (spread(step).equals(this)) {
                return Optional.of(step);
            }
        }
        return Optional.empty();
    }

    /** One plant's step, drawn from the odds. Odds that are all zero are always neutral, and take no draw. */
    public Resistance roll(RandomSource random) {
        int total = veryWeak + weak + neutral + resistant + veryResistant;
        if (total <= 0) {
            return Resistance.NEUTRAL;
        }
        int pick = random.nextInt(total);
        for (Resistance step : Resistance.values()) {
            pick -= weight(step);
            if (pick < 0) {
                return step;
            }
        }
        return Resistance.NEUTRAL;
    }
}
