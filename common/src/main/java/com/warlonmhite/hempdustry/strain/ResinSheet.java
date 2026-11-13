package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

/**
 * A strain's resin: the weight of each {@link ResinStep} a plant of it rolls at planting, very low first, and the
 * {@link ResinTexture} that decides which route a rich plant's bonus pays. The weights need not add up to 100.
 *
 * <p>Written in a strain's {@code plant} object as
 * {@code "resin": {"rich": 20, "very_rich": 80, "texture": "dry"}}: a weight left out is 0, a texture left out is
 * even. Odds that are all zero roll average. A strain with no {@code resin} always rolls average, even: the plant
 * every strain grew before resin, so a datapack strain written for 2.1 sieves and rubs exactly as it did.
 */
public record ResinSheet(int veryLow, int low, int average, int rich, int veryRich, ResinTexture texture) {

    /** Always average, even: a strain that names no resin. */
    public static final ResinSheet DEFAULT = new ResinSheet(0, 0, 100, 0, 0, ResinTexture.EVEN);

    public static final Codec<ResinSheet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 1000).optionalFieldOf("very_low", 0).forGetter(ResinSheet::veryLow),
            Codec.intRange(0, 1000).optionalFieldOf("low", 0).forGetter(ResinSheet::low),
            Codec.intRange(0, 1000).optionalFieldOf("average", 0).forGetter(ResinSheet::average),
            Codec.intRange(0, 1000).optionalFieldOf("rich", 0).forGetter(ResinSheet::rich),
            Codec.intRange(0, 1000).optionalFieldOf("very_rich", 0).forGetter(ResinSheet::veryRich),
            ResinTexture.CODEC.optionalFieldOf("texture", ResinTexture.EVEN).forGetter(ResinSheet::texture)
    ).apply(instance, ResinSheet::new));

    /** The weight of {@code step} among the five. */
    public int weight(ResinStep step) {
        return switch (step) {
            case VERY_LOW -> veryLow;
            case LOW -> low;
            case AVERAGE -> average;
            case RICH -> rich;
            case VERY_RICH -> veryRich;
        };
    }

    /** One plant's step, drawn from the odds. Odds that are all zero are always average, and take no draw. */
    public ResinStep roll(RandomSource random) {
        int total = veryLow + low + average + rich + veryRich;
        if (total <= 0) {
            return ResinStep.AVERAGE;
        }
        int pick = random.nextInt(total);
        for (ResinStep step : ResinStep.values()) {
            pick -= weight(step);
            if (pick < 0) {
                return step;
            }
        }
        return ResinStep.AVERAGE;
    }
}
