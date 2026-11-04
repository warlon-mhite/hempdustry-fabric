package com.warlonmhite.hempdustry.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * The registries a stack's values resolve against on 1.20.1, where an item stack, unlike on the newer
 * lines, is read without any: a packed device names its strain by id, and the id only means something
 * against a world's strain registry.
 *
 * <p>Each side keeps its own, because the two can differ — a client joined to a server holds the
 * server's synced copy, and an integrated server runs beside its client in one process. Code on the
 * server's thread reads the server's; everything else (rendering, tooltips, the recipe viewers) reads
 * the client's, falling back to the server's for the tests and other server-only callers. The server's
 * is kept from the loaders' server lifecycle events ({@code Hempdustry.init}); the client's is asked of
 * the client's connection ({@code HempdustryClient.init}).
 */
public final class StackRegistries {

    private static volatile @Nullable MinecraftServer server;
    private static volatile Supplier<@Nullable RegistryAccess> client = () -> null;
    private static volatile @Nullable HolderLookup.Provider generating;
    private static final ThreadLocal<HolderLookup.@Nullable Provider> OVERRIDE = new ThreadLocal<>();

    private StackRegistries() {
    }

    public static void serverStarting(MinecraftServer started) {
        server = started;
    }

    public static void serverStopped() {
        server = null;
    }

    /** Where the client's registries are found: its connection's, while it is connected to a world. */
    public static void client(Supplier<@Nullable RegistryAccess> registries) {
        client = registries;
    }

    /**
     * The registries a data generator run resolves against: no world is loaded then, and the recipes
     * it writes carry strains in their results.
     */
    public static void dataGeneration(HolderLookup.Provider registries) {
        generating = registries;
    }

    /**
     * Runs {@code work} with {@code registries} as this thread's, whatever its side: for code handed a
     * lookup of its own, as the creative tab is, which on the newer lines writes its stacks' strains
     * against that lookup.
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

    /** The registries for the calling thread's side, or {@code null} when no world is loaded. */
    public static @Nullable HolderLookup.Provider current() {
        HolderLookup.Provider override = OVERRIDE.get();
        if (override != null) {
            return override;
        }
        MinecraftServer running = server;
        if (running != null && running.isSameThread()) {
            return running.registryAccess();
        }
        RegistryAccess joined = client.get();
        if (joined != null) {
            return joined;
        }
        return running != null ? running.registryAccess() : generating;
    }
}
