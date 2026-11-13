package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

/**
 * A strain's resistance sheet: for each {@link StressPhenomenon}, the odds of each {@link Resistance} step a
 * plant of it rolls at planting.
 *
 * <p>Written in a strain's {@code plant} object as {@code "resistances": {"dry_ground": "neutral",
 * "overwatering": "resistant", "light": "neutral", "mildew": "weak"}}, each phenomenon as its most likely step
 * or as five weights ({@link ResistanceOdds}). <b>A phenomenon the file does not name is always neutral</b>, and a
 * strain with no {@code resistances} is neutral on all four: the plant every strain grew before resistances,
 * so a datapack strain written for 2.1 stresses exactly as it did. A strain that wants the shared spread around
 * neutral names it: {@code "light": "neutral"}.
 */
public record Resistances(ResistanceOdds dryGround, ResistanceOdds overwatering, ResistanceOdds light,
                          ResistanceOdds mildew) {

    /** Neutral on everything, always: a strain that names no resistances. */
    public static final Resistances DEFAULT = new Resistances(ResistanceOdds.ALWAYS_NEUTRAL,
            ResistanceOdds.ALWAYS_NEUTRAL, ResistanceOdds.ALWAYS_NEUTRAL, ResistanceOdds.ALWAYS_NEUTRAL);

    public static final Codec<Resistances> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResistanceOdds.CODEC.optionalFieldOf("dry_ground", ResistanceOdds.ALWAYS_NEUTRAL).forGetter(Resistances::dryGround),
            ResistanceOdds.CODEC.optionalFieldOf("overwatering", ResistanceOdds.ALWAYS_NEUTRAL).forGetter(Resistances::overwatering),
            ResistanceOdds.CODEC.optionalFieldOf("light", ResistanceOdds.ALWAYS_NEUTRAL).forGetter(Resistances::light),
            ResistanceOdds.CODEC.optionalFieldOf("mildew", ResistanceOdds.ALWAYS_NEUTRAL).forGetter(Resistances::mildew)
    ).apply(instance, Resistances::new));

    /** A sheet of most likely steps, each spread by the shared spread: how every built-in strain is written. */
    public static Resistances of(Resistance dryGround, Resistance overwatering, Resistance light, Resistance mildew) {
        return new Resistances(ResistanceOdds.spread(dryGround), ResistanceOdds.spread(overwatering),
                ResistanceOdds.spread(light), ResistanceOdds.spread(mildew));
    }

    /** The odds for {@code phenomenon}. */
    public ResistanceOdds odds(StressPhenomenon phenomenon) {
        return switch (phenomenon) {
            case DRY_GROUND -> dryGround;
            case OVERWATERING -> overwatering;
            case LIGHT -> light;
            case MILDEW -> mildew;
        };
    }

    /** One plant's four steps, each drawn on its own from its odds. */
    public ResistanceGenes roll(RandomSource random) {
        return new ResistanceGenes(dryGround.roll(random), overwatering.roll(random), light.roll(random),
                mildew.roll(random));
    }
}
