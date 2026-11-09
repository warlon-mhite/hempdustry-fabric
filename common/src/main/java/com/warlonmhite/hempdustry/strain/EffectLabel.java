package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * What an effect comes from, which decides what makes it stronger.
 *
 * <ul>
 *   <li><b>{@link #THC}</b> — the high and its costs. Its level follows the THC dose, so a CBD-dominant
 *       bud gives none of it and a balanced one about half.</li>
 *   <li><b>{@link #CBD}</b> — flat in level; how long it lasts follows the CBD share.</li>
 *   <li><b>{@link #STRAIN}</b> — the plant's own trait (Beldía's Invisibility, Warped Kush's Fire
 *       Resistance), flat whatever the chemotype: it is the cultivar, not the cannabinoid.</li>
 *   <li><b>{@link #SMOKE}</b> — the smoke itself (schwag's Poison). Its level follows how much is
 *       burned, whatever the chemotype.</li>
 * </ul>
 *
 * <p>An effect written before labels existed takes {@link #THC} if it {@code scales} and
 * {@link #STRAIN} if it does not, which is what every one of 2.1's effects meant.
 */
public enum EffectLabel implements StringRepresentable {
    THC("thc"),
    CBD("cbd"),
    STRAIN("strain"),
    SMOKE("smoke");

    public static final Codec<EffectLabel> CODEC = StringRepresentable.fromEnum(EffectLabel::values);

    private final String name;

    EffectLabel(String name) {
        this.name = name;
    }

    /** Whether a bigger load raises its level: the THC dose for {@link #THC}, the burned count for {@link #SMOKE}. */
    public boolean scales() {
        return this == THC || this == SMOKE;
    }

    /** The label 2.1's {@code scales} flag stood for. */
    public static EffectLabel fromScales(boolean scales) {
        return scales ? THC : STRAIN;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
