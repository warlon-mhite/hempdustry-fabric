package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.util.StringIdentifiable;

/**
 * Which slice of a three-tall plant a block is. The direct analogue of vanilla's
 * {@code DoubleBlockHalf} (which only has LOWER/UPPER, so it can't describe a
 * three-block stack) — see {@link SativaCropBlock}, the only user today.
 *
 * <p>The canonical age of the plant lives on the LOWER segment; MIDDLE and UPPER
 * mirror it.
 */
public enum TriplePlantSegment implements StringIdentifiable {
    LOWER("lower"),
    MIDDLE("middle"),
    UPPER("upper");

    private final String name;

    TriplePlantSegment(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.asString();
    }
}
