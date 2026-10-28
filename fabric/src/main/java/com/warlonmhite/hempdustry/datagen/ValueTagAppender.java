package com.warlonmhite.hempdustry.datagen;

import net.minecraft.core.Registry;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;

/**
 * A tag builder that takes the blocks and items themselves, as the tag providers' value-lookup
 * builder did up to 1.21.11. Since 26.2 a tag provider takes registry keys only; this hands each
 * value over as its key, so the providers read the same on every line the mod is built for and a
 * tag change made on one carries to the others as it is.
 */
public final class ValueTagAppender<T> {
    private final TagAppender<T> keys;
    private final Registry<T> registry;

    public ValueTagAppender(TagAppender<T> keys, Registry<T> registry) {
        this.keys = keys;
        this.registry = registry;
    }

    public ValueTagAppender<T> add(T value) {
        keys.add(registry.getResourceKey(value).orElseThrow(
                () -> new IllegalStateException(value + " is not registered in " + registry.key().identifier())));
        return this;
    }

    @SafeVarargs
    public final ValueTagAppender<T> add(T... values) {
        for (T value : values) {
            add(value);
        }
        return this;
    }

    public ValueTagAppender<T> addTag(TagKey<T> tag) {
        keys.addTag(tag);
        return this;
    }

    public ValueTagAppender<T> addOptionalTag(TagKey<T> tag) {
        keys.addOptionalTag(tag);
        return this;
    }

    /** A tag entry that is written even where its tag has nothing of the mod's in it. */
    public ValueTagAppender<T> forceAddTag(TagKey<T> tag) {
        keys.forceAddTag(tag);
        return this;
    }
}
