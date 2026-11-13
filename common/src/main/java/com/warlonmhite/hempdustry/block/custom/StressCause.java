package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.strain.ResistanceGenes;
import com.warlonmhite.hempdustry.strain.StressPhenomenon;

/**
 * One cause of stress and its chance against a neutral plant: a dry bed, a dead pump, a bottle into a wet pot,
 * bone meal in a fed bed. Each belongs to a {@link StressPhenomenon}, so a plant's own resistance to that
 * phenomenon scales it ({@link #chanceFor}).
 */
public record StressCause(StressPhenomenon phenomenon, float chance) {

    /** Nothing that can stress a plant. */
    public static final StressCause NONE = new StressCause(StressPhenomenon.DRY_GROUND, 0.0F);

    /** A cause of dry ground at {@code chance}. */
    public static StressCause dryGround(float chance) {
        return new StressCause(StressPhenomenon.DRY_GROUND, chance);
    }

    /** A cause of overwatering or overfeeding at {@code chance}. */
    public static StressCause overwatering(float chance) {
        return new StressCause(StressPhenomenon.OVERWATERING, chance);
    }

    /** This cause's chance against a plant with {@code genes}: its step against the phenomenon multiplies it. */
    public float chanceFor(ResistanceGenes genes) {
        return chance > 0.0F ? genes.get(phenomenon).apply(chance) : 0.0F;
    }
}
