package com.warlonmhite.hempdustry.util;

/** NBT type ids, by name: 1.17 named them on {@code Tag} ({@code Tag.TAG_COMPOUND}); 1.16.5 has bare numbers. */
public final class NbtTypes {

    public static final int STRING = 8;
    public static final int LIST = 9;
    public static final int COMPOUND = 10;
    /** Any number, as {@code CompoundTag#contains} reads it. */
    public static final int ANY_NUMERIC = 99;

    private NbtTypes() {
    }
}
