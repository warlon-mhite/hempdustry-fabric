package com.warlonmhite.hempdustry.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

/**
 * Where a loot table came from: with the game or a mod, or from a datapack put in its place. The mod
 * adds its loot to the first kind only, on both loaders, by the same rule.
 *
 * <p>The game knows its own packs by name, and only those: vanilla's main pack ({@code minecraft:core}),
 * each of Mojang's experiment packs ({@code minecraft:trade_rebalance}, which replaces five chests),
 * and every mod's pack on both loaders. A datapack in a world's or a server's folder has no such name
 * ({@code FolderRepositorySource} builds it with none). So the copy of a table that won is asked which
 * pack it came from, and whether the game knows that pack.
 */
public final class LootTableOrigin {
    /** The resources the loot tables are being read from, kept by {@code ReloadableServerRegistriesMixin}. */
    public static volatile @Nullable ResourceManager loading;

    private LootTableOrigin() {
    }

    /** Whether the copy of {@code table} being loaded now came with the game or with a mod. */
    public static boolean fromTheGameOrAMod(ResourceLocation table) {
        ResourceManager resources = loading;
        return resources == null || fromTheGameOrAMod(resources, table);
    }

    /**
     * Whether the copy of {@code table} that wins in {@code resources} came with the game or with a mod.
     * A table with no file at all was built in code by whoever registered it, and counts as theirs.
     */
    public static boolean fromTheGameOrAMod(ResourceManager resources, ResourceLocation table) {
        return resources.getResource(ResourceLocation.fromNamespaceAndPath(table.getNamespace(), "loot_table/" + table.getPath() + ".json"))
                .map(LootTableOrigin::fromTheGameOrAMod)
                .orElse(true);
    }

    /** Whether {@code file} came from a pack the game knows by name. */
    public static boolean fromTheGameOrAMod(Resource file) {
        return file.source().location().knownPackInfo().isPresent();
    }
}
