package com.warlonmhite.hempdustry.balance;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The numbers a datapack can rebalance without touching the code: how each smokeable smokes
 * ({@link DeviceStats}), what an edible does ({@link EdibleBundle}) and what a green-out costs
 * ({@link GreenOut}). Three dynamic registries in the image of the strain one, loaded per world from
 * {@code data/<namespace>/hempdustry/<device|edible|green_out>/<id>.json}.
 *
 * <p>The mod ships its own numbers as those files, written by datagen from the built-in values, so a
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

    private static final Set<RegistryKey<?>> WARNED = ConcurrentHashMap.newKeySet();

    /**
     * Synced, like the strains: the creative tab and the recipe viewers show the bowls the server
     * actually packs.
     */
    public static void registerBalance() {
        DynamicRegistries.registerSynced(DeviceStats.REGISTRY_KEY, DeviceStats.CODEC);
        DynamicRegistries.registerSynced(EdibleBundle.REGISTRY_KEY, EdibleBundle.CODEC);
        DynamicRegistries.registerSynced(GreenOut.REGISTRY_KEY, GreenOut.CODEC);
        Hempdustry.LOGGER.info("Registering Balance for " + Hempdustry.MOD_ID);
    }

    /**
     * The loaded entry for {@code key}, or {@code builtIn} when this world has none. The mod's own
     * files are always loaded, so a miss means its data pack was switched off. The game carries on
     * with the numbers the mod shipped with, and says so once rather than on every hit.
     */
    static <T> T entry(RegistryWrapper.WrapperLookup registries, RegistryKey<T> key, T builtIn) {
        return registries.getOptional(key.getRegistryRef())
                .flatMap(registry -> registry.getOptional(key))
                .map(RegistryEntry.Reference::value)
                .orElseGet(() -> {
                    if (WARNED.add(key)) {
                        Hempdustry.LOGGER.warn("{} is missing from this world's data packs; using the built-in numbers", key);
                    }
                    return builtIn;
                });
    }
}
