package com.warlonmhite.hempdustry.component;

import com.warlonmhite.hempdustry.registry.DataRegistries;
import com.warlonmhite.hempdustry.registry.HolderLookup;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * The registries a stack's values resolve against, where an item stack, unlike on the newer lines, is
 * read without any: a packed device names its strain by id, and the id only means something against a
 * world's strain registry.
 *
 * <p>Each side keeps its own, because the two can differ — a client joined to a server holds the
 * server's synced copy, and an integrated server runs beside its client in one process. Code on the
 * server's thread reads the server's; everything else (rendering, tooltips, the recipe viewer) reads
 * the client's while it is in a world, falling back to the server's for server-only callers. The server
 * is kept from the loader's server lifecycle events ({@code ModComponents}); whether the client is in a
 * world is asked of the client ({@code HempdustryClient.init}).
 */
public final class StackRegistries {

    private static volatile @Nullable MinecraftServer server;
    private static volatile BooleanSupplier clientInWorld = () -> false;
    private static final ThreadLocal<HolderLookup.@Nullable Provider> OVERRIDE = new ThreadLocal<>();

    private StackRegistries() {
    }

    public static void serverStarting(MinecraftServer started) {
        server = started;
    }

    public static void serverStopped() {
        server = null;
    }

    /** Whether the client is connected to a world, and so holds what its server sent. */
    public static void client(BooleanSupplier inWorld) {
        clientInWorld = inWorld;
    }

    /**
     * Runs {@code work} with {@code registries} as this thread's, whatever its side: for code handed a
     * lookup of its own, as the creative tab is.
     */
    public static void with(HolderLookup.Provider registries, Runnable work) {
        HolderLookup.Provider outer = OVERRIDE.get();
        OVERRIDE.set(registries);
        try {
            work.run();
        } finally {
            OVERRIDE.set(outer);
        }
    }

    /** The registries for this thread: see the class javadoc. Null before any world is loaded. */
    public static HolderLookup.@Nullable Provider current() {
        HolderLookup.Provider override = OVERRIDE.get();
        if (override != null) {
            return override;
        }
        MinecraftServer running = server;
        if (running != null && running.isSameThread()) {
            return DataRegistries.SERVER;
        }
        if (clientInWorld.getAsBoolean()) {
            return DataRegistries.CLIENT;
        }
        return running != null ? DataRegistries.SERVER : null;
    }
}
