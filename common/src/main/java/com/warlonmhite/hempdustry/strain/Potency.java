package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

/**
 * A strain's potency sheet: what one of its buds is worth at the oven, and the odds of each
 * {@link PotencyBand} a plant of it rolls.
 *
 * <p>{@code base} is a mid bud's decarboxylated hemp, the number the bowl weighs a bud by (a bud of
 * base 4 is one dose of THC); every strain the mod ships has 4, which is what a bud baked into in 2.1.
 * The odds are weights, read in the order low, mid, high, extra-high; they need not add up to 100.
 * Extra-high is for breeders: rare or impossible on a native plant.
 *
 * <p>Written in a strain's file as {@code "potency": {"base": 4, "low": 25, "mid": 50, "high": 25,
 * "extra_high": 0}}; every key is optional, and a strain with no {@code potency} at all reads as
 * {@link #DEFAULT}.
 */
public record Potency(int base, int low, int mid, int high, int extraHigh) {

    /** A mid bud's decarboxylated hemp, for every strain that names none. */
    public static final int DEFAULT_BASE = 4;

    /** Purple Kush's and Lemon Haze's sheet, and any strain's that names none. */
    public static final Potency DEFAULT = new Potency(DEFAULT_BASE, 25, 50, 25, 0);

    public static final Codec<Potency> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 64).optionalFieldOf("base", DEFAULT.base).forGetter(Potency::base),
            Codec.intRange(0, 1000).optionalFieldOf("low", DEFAULT.low).forGetter(Potency::low),
            Codec.intRange(0, 1000).optionalFieldOf("mid", DEFAULT.mid).forGetter(Potency::mid),
            Codec.intRange(0, 1000).optionalFieldOf("high", DEFAULT.high).forGetter(Potency::high),
            Codec.intRange(0, 1000).optionalFieldOf("extra_high", DEFAULT.extraHigh).forGetter(Potency::extraHigh)
    ).apply(instance, Potency::new));

    /** A sheet at the default base with these odds. */
    public static Potency odds(int low, int mid, int high, int extraHigh) {
        return new Potency(DEFAULT_BASE, low, mid, high, extraHigh);
    }

    /** The weight of {@code band} among the four. */
    public int weight(PotencyBand band) {
        return switch (band) {
            case LOW -> low;
            case MID -> mid;
            case HIGH -> high;
            case EXTRA_HIGH -> extraHigh;
        };
    }

    /** One plant's band, drawn from the odds. A sheet whose odds are all zero is always mid. */
    public PotencyBand roll(RandomSource random) {
        int total = low + mid + high + extraHigh;
        if (total <= 0) {
            return PotencyBand.MID;
        }
        int pick = random.nextInt(total);
        for (PotencyBand band : PotencyBand.values()) {
            pick -= weight(band);
            if (pick < 0) {
                return band;
            }
        }
        return PotencyBand.MID;
    }

    /** Decarboxylated hemp in one bud of {@code band}: the base moved by the band, never below one. */
    public int decarbCount(PotencyBand band) {
        return Math.max(1, base + band.offset());
    }
}
