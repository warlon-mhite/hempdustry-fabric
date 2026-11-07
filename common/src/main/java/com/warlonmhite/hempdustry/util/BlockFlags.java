package com.warlonmhite.hempdustry.util;

/**
 * The flags of {@code Level#setBlock}, by name. Minecraft 1.17 named them on {@code Block}
 * ({@code Block.UPDATE_CLIENTS} and the rest); 1.16.5 takes the same bits as bare numbers.
 */
public final class BlockFlags {

    /** Tells the neighbours the block changed. */
    public static final int UPDATE_NEIGHBORS = 1;
    /** Sends the change to the clients. */
    public static final int UPDATE_CLIENTS = 2;
    /** Both of the above: vanilla's usual {@code 3}. */
    public static final int UPDATE_ALL = UPDATE_NEIGHBORS | UPDATE_CLIENTS;
    /** Keeps the client from re-rendering the block. */
    public static final int UPDATE_INVISIBLE = 4;
    /** Has the client re-render the block on the main thread, at once. */
    public static final int UPDATE_IMMEDIATE = 8;
    /** {@link #UPDATE_ALL} and {@link #UPDATE_IMMEDIATE}: vanilla's {@code 11}. */
    public static final int UPDATE_ALL_IMMEDIATE = UPDATE_ALL | UPDATE_IMMEDIATE;
    /** Skips the neighbours' shape updates. */
    public static final int UPDATE_KNOWN_SHAPE = 16;
    /** Keeps a neighbour's reaction to the change from dropping items. */
    public static final int UPDATE_SUPPRESS_DROPS = 32;

    private BlockFlags() {
    }
}
