package com.warlonmhite.hempdustry.registry;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Where the mod's datapack registries are read from, as the newer lines' vanilla
 * {@code HolderLookup}: on 1.16.5 a {@link DataRegistries} -- the server's, loaded from its datapacks,
 * or the client's, as the server sent it.
 */
public interface HolderLookup {

    /** Every registry a side has. */
    interface Provider {
        <T> Optional<RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> registry);

        default <T> RegistryLookup<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> registry) {
            return this.<T>lookup(registry).orElseThrow(() -> new IllegalStateException("Missing registry: " + registry));
        }
    }

    /** One registry's entries. */
    interface RegistryLookup<T> {
        ResourceKey<? extends Registry<? extends T>> key();

        Stream<Holder.Reference<T>> listElements();

        Optional<Holder.Reference<T>> get(ResourceKey<T> key);

        default Holder.Reference<T> getOrThrow(ResourceKey<T> key) {
            return get(key).orElseThrow(() -> new IllegalStateException("Missing " + key));
        }
    }
}
