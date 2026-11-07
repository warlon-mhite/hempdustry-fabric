package com.warlonmhite.hempdustry.loot;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.jetbrains.annotations.Nullable;

/**
 * Where a loot table came from: with the game or a mod, or from a datapack put in its place. The mod
 * adds its loot to the first kind only, by the same rule on every line.
 *
 * <p>On 1.18.2 a datapack is known by the kind of pack it is, not by its name: the game opens each
 * datapack of a world's (or a server's) folder as a {@link FolderPackResources} or, zipped, a
 * {@link FilePackResources}, and nothing else as either -- vanilla's own pack, Forge's and every mod's
 * jar are packs of other kinds. The names do not tell them apart: the pack is listed as
 * {@code file/<folder>}, but the files it serves name the folder alone as their source. So the pack
 * whose copy of a table wins is found, and asked what kind it is.
 */
public final class LootTableOrigin {
    /** The resources the loot tables are being read from, kept by {@code LootTablesMixin}. */
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
        ResourceLocation file = new ResourceLocation(table.getNamespace(), "loot_tables/" + table.getPath() + ".json");
        if (!(resources instanceof MultiPackResourceManager)) {
            // Not the server's kind of manager, so its packs cannot be asked: count the table as the
            // game's, as the newer lines do a table with no file.
            return true;
        }
        FallbackResourceManager namespace = ((MultiPackResourceManager) resources).namespacedManagers.get(table.getNamespace());
        if (namespace == null) {
            return true;
        }
        // Packs are kept in the order they were added, and a later pack's file wins over an earlier
        // one's (FallbackResourceManager reads them last to first).
        List<PackResources> packs = namespace.fallbacks;
        for (int i = packs.size() - 1; i >= 0; i--) {
            PackResources pack = packs.get(i);
            if (pack.hasResource(PackType.SERVER_DATA, file)) {
                return !(pack instanceof FolderPackResources || pack instanceof FilePackResources);
            }
        }
        return true;
    }
}
