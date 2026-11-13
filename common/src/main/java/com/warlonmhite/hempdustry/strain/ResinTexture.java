package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * What a strain's resin is like, and so which route its extra resin pays: the sieve, the rub, or half of each.
 * Hash makers tell good washers (dry heads that snap off the plant) from greasy, sticky resin that rubs and
 * presses well. A strain's, fixed for now.
 *
 * <p>Only the <em>bonus</em> of a rich plant is shared out; the penalty of a low one applies in full on both
 * routes ({@link ResinStep#sieveFactor}, {@link ResinStep#rubChance}).
 *
 * <table>
 *   <tr><th>texture</th><th>sieve</th><th>rub</th></tr>
 *   <tr><td>dry</td><td>full bonus</td><td>no bonus</td></tr>
 *   <tr><td>even</td><td>half</td><td>half</td></tr>
 *   <tr><td>sticky</td><td>half</td><td>full bonus</td></tr>
 * </table>
 */
public enum ResinTexture implements StringRepresentable {
    DRY("dry", 1.0F, 0.0F),
    EVEN("even", 0.5F, 0.5F),
    STICKY("sticky", 0.5F, 1.0F);

    public static final Codec<ResinTexture> CODEC = StringRepresentable.fromEnum(ResinTexture::values);

    private final String name;
    private final float sieveShare;
    private final float rubShare;

    ResinTexture(String name, float sieveShare, float rubShare) {
        this.name = name;
        this.sieveShare = sieveShare;
        this.rubShare = rubShare;
    }

    /** How much of a rich plant's bonus the sieve pays: 0 to 1. */
    public float sieveShare() {
        return sieveShare;
    }

    /** How much of a rich plant's bonus the rub pays: 0 to 1. */
    public float rubShare() {
        return rubShare;
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
