package com.warlonmhite.hempdustry.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One of the mod's values carried on an item stack — what Minecraft 1.20.5 and later call a data
 * component, kept on 1.20.1 as an entry of the stack's tag.
 *
 * <p>The entry's key is the component's id ({@code hempdustry:charges}), and the value is written with
 * the very codec the component has on the newer lines. That is what makes a 1.20.1 world upgrade
 * cleanly: Minecraft's 1.20.5 data fixer moves every tag entry it does not know into the stack's
 * {@code minecraft:custom_data} unchanged, so the newer lines find each value under its own id, in its
 * own shape, and only have to lift it out.
 *
 * <p>A value that does not decode — a strain a datapack no longer defines — reads as absent, and the
 * stack keeps the raw entry: nothing is written back until the mod sets a new value.
 */
public final class ModComponent<T> {

    private final ResourceLocation id;
    private final String key;
    private final Codec<T> codec;

    ModComponent(ResourceLocation id, Codec<T> codec) {
        this.id = id;
        this.key = id.toString();
        this.codec = codec;
    }

    public ResourceLocation id() {
        return id;
    }

    /** The key the value is kept under in the stack's tag. */
    public String key() {
        return key;
    }

    public Codec<T> codec() {
        return codec;
    }

    /** The value on this stack, or {@code null} when it has none (or one that does not decode). */
    public @Nullable T get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) {
            return null;
        }
        return decode(tag.get(key));
    }

    public T getOrDefault(ItemStack stack, T fallback) {
        T value = get(stack);
        return value != null ? value : fallback;
    }

    /** Whether the stack carries this value — the raw entry, decodable or not. */
    public boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key);
    }

    /** Sets the value; {@code null} removes it. */
    public void set(ItemStack stack, @Nullable T value) {
        if (value == null) {
            remove(stack);
            return;
        }
        Tag encoded = codec.encodeStart(ops(), value)
                .getOrThrow(false, message -> {
                    throw new IllegalStateException("Cannot write " + key + ": " + message);
                });
        stack.getOrCreateTag().put(key, encoded);
    }

    public void update(ItemStack stack, T fallback, UnaryOperator<T> change) {
        set(stack, change.apply(getOrDefault(stack, fallback)));
    }

    public void remove(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(key);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    /** The value as written in a tag, or {@code null} — for a reader that holds a tag, not a stack. */
    public @Nullable T decode(@Nullable Tag tag) {
        if (tag == null) {
            return null;
        }
        return codec.parse(ops(), tag).result().orElse(null);
    }

    /**
     * Plain NBT: a strain id resolves against the loaded strains through its own codec
     * ({@code Strain.ENTRY_CODEC}, which asks {@link StackRegistries}), so no registry-aware ops are
     * needed on 1.16.5.
     */
    private static DynamicOps<Tag> ops() {
        return NbtOps.INSTANCE;
    }

    @Override
    public String toString() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ModComponent<?> that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
