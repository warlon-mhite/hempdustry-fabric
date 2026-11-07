package com.warlonmhite.hempdustry.registry;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;

/**
 * An entry of one of the mod's datapack registries -- a strain, a device's stats -- as the newer
 * lines' vanilla {@code Holder} is: a value, and the key it was loaded under. Minecraft 1.18.2 has no
 * datapack registries a mod can add, so {@link DataRegistries} loads and syncs them; this keeps the
 * code reading them the same as on the newer lines.
 */
public interface Holder<T> {

    T value();

    /** The key this entry was loaded under; empty for a value made in code ({@link #direct}). */
    Optional<ResourceKey<T>> unwrapKey();

    default boolean is(ResourceKey<T> key) {
        return unwrapKey().map(key::equals).orElse(false);
    }

    /** A value that belongs to no registry, as vanilla's {@code Holder.direct}. */
    static <T> Holder<T> direct(T value) {
        return new Holder<T>() {
            @Override
            public T value() {
                return value;
            }

            @Override
            public Optional<ResourceKey<T>> unwrapKey() {
                return Optional.empty();
            }
        };
    }

    /** An entry loaded under a key. */
    final class Reference<T> implements Holder<T> {
        private final ResourceKey<T> key;
        private final T value;

        public Reference(ResourceKey<T> key, T value) {
            this.key = key;
            this.value = value;
        }

        public ResourceKey<T> key() {
            return key;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.of(key);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Reference<?> that && key.equals(that.key) && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "Reference{" + key + "=" + value + "}";
        }
    }
}
