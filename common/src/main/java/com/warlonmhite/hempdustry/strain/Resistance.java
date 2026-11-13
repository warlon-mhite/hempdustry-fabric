package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/**
 * How hardy one plant is against one {@link StressPhenomenon}, as one of five steps rolled at planting
 * from its strain's odds ({@link ResistanceOdds}). Hidden: no seed, plant or bud shows it; it is felt only
 * as how often the plant stresses.
 *
 * <ul>
 *   <li><b>A step multiplies a cause's chance</b> ({@link #factor}): very weak ×2, weak ×1.5, neutral ×1,
 *       resistant ×½, very resistant ×¼. A dry bed's 1 in 3 becomes 2 in 3, 1 in 2, 1 in 3, 1 in 6, 1 in 12.
 *       A resistant plant is never invincible: it still needs water.</li>
 *   <li><b>A lost light</b> is certain, so for light the step reads as how often the plant ignores the loss
 *       ({@link #ignoresLostLight}: resistant 1 time in 2, very resistant 3 in 4) and from which age a loss
 *       stresses it ({@link #lightLossAge}: weak and very weak from age 3, everyone else from age 4).</li>
 * </ul>
 *
 * <p>The constants are in order, weakest first; {@link #score} is a step's place on the hardiness scale.
 */
public enum Resistance implements StringRepresentable {
    VERY_WEAK("very_weak", -2, 2.0F, 0.0F),
    WEAK("weak", -1, 1.5F, 0.0F),
    NEUTRAL("neutral", 0, 1.0F, 0.0F),
    RESISTANT("resistant", 1, 0.5F, 0.5F),
    VERY_RESISTANT("very_resistant", 2, 0.25F, 0.75F);

    public static final Codec<Resistance> CODEC = StringRepresentable.fromEnum(Resistance::values);

    /** The age from which a lost light stresses a weak or very weak plant, a stage before flowering. */
    public static final int WEAK_LIGHT_LOSS_AGE = 3;
    /** The age from which a lost light stresses every other plant: flowering ({@code GrowLight#FLOWERING_AGE}). */
    public static final int LIGHT_LOSS_AGE = 4;

    private final String name;
    private final int score;
    private final float factor;
    private final float ignoresLoss;

    Resistance(String name, int score, float factor, float ignoresLoss) {
        this.name = name;
        this.score = score;
        this.factor = factor;
        this.ignoresLoss = ignoresLoss;
    }

    /** This step's place on the hardiness scale: very weak −2 to very resistant +2. */
    public int score() {
        return score;
    }

    /** What this step multiplies a cause's chance by. */
    public float factor() {
        return factor;
    }

    /** {@code chance} for a plant of this step: multiplied by {@link #factor}, never above certain. */
    public float apply(float chance) {
        return Math.min(1.0F, chance * factor);
    }

    /** The age from which losing its light stresses a plant of this step. */
    public int lightLossAge() {
        return this.compareTo(NEUTRAL) < 0 ? WEAK_LIGHT_LOSS_AGE : LIGHT_LOSS_AGE;
    }

    /**
     * Whether a plant of this step shrugs off a lost light this time. Draws from {@code random} only for a
     * resistant or very resistant plant, so every other plant's step takes the draws it always did.
     */
    public boolean ignoresLostLight(RandomSource random) {
        return ignoresLoss > 0.0F && random.nextFloat() < ignoresLoss;
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
