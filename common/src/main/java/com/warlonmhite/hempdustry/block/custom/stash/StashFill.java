package com.warlonmhite.hempdustry.block.custom.stash;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * How full a Stash Jar looks, against its own capacity: empty, up to a third, up to two thirds, more.
 * One item already reads low. The placed block carries it as its {@code fill} state and the item
 * reads the same level from its contents, so a full jar looks full in the hotbar too.
 */
public enum StashFill implements StringRepresentable {
    EMPTY("empty", 0),
    LOW("low", 1),
    MID("mid", 3),
    FULL("full", 5);

    public static final EnumProperty<StashFill> PROPERTY = EnumProperty.create("fill", StashFill.class);

    private final String name;
    private final int pieces;

    StashFill(String name, int pieces) {
        this.name = name;
        this.pieces = pieces;
    }

    /** The level of {@code count} items in a jar of {@code capacity}. */
    public static StashFill of(int count, int capacity) {
        if (count <= 0) {
            return EMPTY;
        }
        // Whole numbers, so 21 of 64 is low and 22 is mid with no rounding in between.
        if (count * 3L <= capacity) {
            return LOW;
        }
        return count * 3L <= capacity * 2L ? MID : FULL;
    }

    /** How many stash pieces the model shows at this level. */
    public int pieces() {
        return pieces;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
