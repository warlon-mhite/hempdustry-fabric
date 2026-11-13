package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One plant's four resistances, rolled at planting from its strain's {@link Resistances} and kept on its record
 * until it is harvested or set back to age 0. Hidden: felt only as how often the plant stresses.
 */
public record ResistanceGenes(Resistance dryGround, Resistance overwatering, Resistance light, Resistance mildew) {

    /** A plant with no record to ask: neutral on everything, as every plant was before resistances. */
    public static final ResistanceGenes NEUTRAL = new ResistanceGenes(Resistance.NEUTRAL, Resistance.NEUTRAL,
            Resistance.NEUTRAL, Resistance.NEUTRAL);

    public static final Codec<ResistanceGenes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Resistance.CODEC.optionalFieldOf("dry_ground", Resistance.NEUTRAL).forGetter(ResistanceGenes::dryGround),
            Resistance.CODEC.optionalFieldOf("overwatering", Resistance.NEUTRAL).forGetter(ResistanceGenes::overwatering),
            Resistance.CODEC.optionalFieldOf("light", Resistance.NEUTRAL).forGetter(ResistanceGenes::light),
            Resistance.CODEC.optionalFieldOf("mildew", Resistance.NEUTRAL).forGetter(ResistanceGenes::mildew)
    ).apply(instance, ResistanceGenes::new));

    /** This plant's step against {@code phenomenon}. */
    public Resistance get(StressPhenomenon phenomenon) {
        return switch (phenomenon) {
            case DRY_GROUND -> dryGround;
            case OVERWATERING -> overwatering;
            case LIGHT -> light;
            case MILDEW -> mildew;
        };
    }

    /** This plant with {@code step} against {@code phenomenon}: what a test or a command does. */
    public ResistanceGenes with(StressPhenomenon phenomenon, Resistance step) {
        return new ResistanceGenes(
                phenomenon == StressPhenomenon.DRY_GROUND ? step : dryGround,
                phenomenon == StressPhenomenon.OVERWATERING ? step : overwatering,
                phenomenon == StressPhenomenon.LIGHT ? step : light,
                phenomenon == StressPhenomenon.MILDEW ? step : mildew);
    }

    /** Every phenomenon at {@code step}. */
    public static ResistanceGenes all(Resistance step) {
        return new ResistanceGenes(step, step, step, step);
    }
}
