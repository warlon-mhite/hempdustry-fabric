package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * How well one plant is made to grow, as one of four steps rolled at planting from its strain's odds
 * ({@link GrowQuality}): poor, average, good and elite. Hidden: no seed, plant or bud shows it. It is
 * felt only at the harvest, where its {@link #luck} joins the plant's grow luck and moves the odds of
 * each {@link BudTier}.
 */
public enum QualityGene implements StringRepresentable {
    POOR("poor", -1),
    AVERAGE("average", 0),
    GOOD("good", 1),
    ELITE("elite", 2);

    public static final Codec<QualityGene> CODEC = StringRepresentable.fromEnum(QualityGene::values);

    private final String name;
    private final int luck;

    QualityGene(String name, int luck) {
        this.name = name;
        this.luck = luck;
    }

    /** What this gene adds to the plant's grow luck. */
    public int luck() {
        return luck;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
