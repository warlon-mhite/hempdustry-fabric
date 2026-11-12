package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

/**
 * A strain's quality sheet: the luck it brings to every harvest of its plants, and the odds of each
 * {@link QualityGene} a plant of it rolls at planting.
 *
 * <p>{@code baseLuck} stands in for the care a strain cannot take: the Nether's plants keep no light
 * record and stand on no Hydro Tray, so theirs is +2; Beldía, a hardy landrace grown for the sieve,
 * has −1. The odds are weights in the order poor, average, good, elite; they need not add up to 100.
 *
 * <p>Written in a strain's file as {@code "quality": {"base_luck": 0, "poor": 20, "average": 50,
 * "good": 25, "elite": 5}}; every key is optional, and a strain with no {@code quality} reads as
 * {@link #DEFAULT}.
 */
public record GrowQuality(int baseLuck, int poor, int average, int good, int elite) {

    /** Purple Kush's sheet, and any strain's that names none. */
    public static final GrowQuality DEFAULT = new GrowQuality(0, 20, 50, 25, 5);

    public static final Codec<GrowQuality> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(-64, 64).optionalFieldOf("base_luck", DEFAULT.baseLuck).forGetter(GrowQuality::baseLuck),
            Codec.intRange(0, 1000).optionalFieldOf("poor", DEFAULT.poor).forGetter(GrowQuality::poor),
            Codec.intRange(0, 1000).optionalFieldOf("average", DEFAULT.average).forGetter(GrowQuality::average),
            Codec.intRange(0, 1000).optionalFieldOf("good", DEFAULT.good).forGetter(GrowQuality::good),
            Codec.intRange(0, 1000).optionalFieldOf("elite", DEFAULT.elite).forGetter(GrowQuality::elite)
    ).apply(instance, GrowQuality::new));

    /** The weight of {@code gene} among the four. */
    public int weight(QualityGene gene) {
        return switch (gene) {
            case POOR -> poor;
            case AVERAGE -> average;
            case GOOD -> good;
            case ELITE -> elite;
        };
    }

    /** One plant's gene, drawn from the odds. A sheet whose odds are all zero is always average. */
    public QualityGene roll(RandomSource random) {
        int total = poor + average + good + elite;
        if (total <= 0) {
            return QualityGene.AVERAGE;
        }
        int pick = random.nextInt(total);
        for (QualityGene gene : QualityGene.values()) {
            pick -= weight(gene);
            if (pick < 0) {
                return gene;
            }
        }
        return QualityGene.AVERAGE;
    }
}
