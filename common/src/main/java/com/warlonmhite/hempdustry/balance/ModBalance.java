package com.warlonmhite.hempdustry.balance;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.registry.Holder;
import com.warlonmhite.hempdustry.registry.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The numbers a datapack can rebalance without touching the code: how each smokeable smokes
 * ({@link DeviceStats}), what an edible does ({@link EdibleBundle}) and what a green-out costs
 * ({@link GreenOut}). Three dynamic registries in the image of the strain one, loaded per world from
 * {@code data/<namespace>/hempdustry/<device|edible|green_out>/<id>.json}.
 *
 * <p>The mod ships its own numbers as those files, holding the built-in values, so a
 * world that never touches them plays exactly as before. The server's config keeps the last word:
 * its multipliers, caps and switches apply on top of whatever a datapack asks for.
 *
 * <p>What stays code: a device's durability and enchantability, because they are item components
 * and a datapack cannot set those for any item, vanilla's included; the four edible tiers, because
 * the tier is an effect amplifier and a block state on the Space Cake; and what a green-out
 * <em>does</em>, as opposed to how much of it.
 */
public final class ModBalance {
    private ModBalance() {
    }

    private static final Set<ResourceKey<?>> WARNED = ConcurrentHashMap.newKeySet();

    /** Its three registries are registered with the strains' ({@link Hempdustry#registerDynamicRegistries}). */
    public static void registerBalance() {
        Hempdustry.LOGGER.info("Registering Balance for " + Hempdustry.MOD_ID);
    }

    /**
     * The loaded entry for {@code key}, or {@code builtIn} when this world has none. The mod's own
     * files are always loaded, so a miss means its data pack was switched off. The game carries on
     * with the numbers the mod shipped with, and says so once rather than on every hit.
     */
    static <T> T entry(HolderLookup.Provider registries, ResourceKey<Registry<T>> registryKey, ResourceKey<T> key,
                       T builtIn) {
        return registries.<T>lookup(registryKey)
                .flatMap(registry -> registry.get(key))
                .map(Holder.Reference::value)
                .orElseGet(() -> {
                    // Only a world's data can be missing an entry: before any world is loaded (a
                    // client at start-up, building its creative search) the registry is empty.
                    boolean loaded = registries.<T>lookup(registryKey)
                            .map(registry -> registry.listElements().findAny().isPresent()).orElse(false);
                    if (loaded && WARNED.add(key)) {
                        Hempdustry.LOGGER.warn("{} is missing from this world's data packs; using the built-in numbers", key);
                    }
                    return builtIn;
                });
    }
}
