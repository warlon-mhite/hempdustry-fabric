package com.warlonmhite.hempdustry.registry;

import com.warlonmhite.hempdustry.util.NbtTypes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.Hempdustry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

/**
 * The mod's datapack registries -- strains, device stats, edible bundles, green-out -- on Minecraft
 * 1.16.5, which has no datapack registries a mod can add. The newer lines declare them to vanilla,
 * which reads them from {@code data/<pack>/hempdustry/<name>/} and sends them to every client; here the
 * server reads the same folders with a reload listener of each registry's own, and sends what it read
 * to each client as it joins and after every {@code /reload} (the loader's packet carries
 * {@link #write}). So a datapack's strain reaches a client exactly as on the newer lines, from the
 * same files.
 *
 * <p>Two sides: {@link #SERVER}, what the server loaded, and {@link #CLIENT}, what the server sent. In
 * single player both live in one game, each read on its own side ({@link #of(Level)}).
 */
public final class DataRegistries implements HolderLookup.Provider {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceKey<?>, Declared<?>> DECLARED = new LinkedHashMap<>();

    public static final DataRegistries SERVER = new DataRegistries("server");
    public static final DataRegistries CLIENT = new DataRegistries("client");

    private final String side;
    private final Map<ResourceKey<?>, Loaded<?>> loaded = new LinkedHashMap<>();

    private DataRegistries(String side) {
        this.side = side;
    }

    /** Declares a registry, read from {@code data/<pack>/<key namespace>/<key path>/*.json}. */
    public static synchronized <T> void declare(ResourceKey<Registry<T>> key, Codec<T> codec) {
        DECLARED.put(key, new Declared<>(key, codec));
    }

    /** The side a level reads: the client's on a client level, the server's otherwise. */
    public static DataRegistries of(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    public static DataRegistries of(MinecraftServer server) {
        return SERVER;
    }

    /** The server's reload listeners, one per declared registry, for the loader to add. */
    public static List<PreparableReloadListener> reloadListeners() {
        List<PreparableReloadListener> out = new ArrayList<>();
        for (Declared<?> declared : DECLARED.values()) {
            out.add(declared.listener());
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> registry) {
        Loaded<?> found = loaded.get(registry);
        if (found == null) {
            // Declared but not loaded yet -- a client before its first server, which vanilla asks for
            // the creative search at start-up: the registry is there, with nothing in it.
            Declared<?> declared = DECLARED.get(registry);
            if (declared != null) {
                found = declared.empty();
            }
        }
        return Optional.ofNullable((HolderLookup.RegistryLookup<T>) found);
    }

    /** What the server loaded, for a client: every registry, every entry, in NBT. */
    public CompoundTag write() {
        CompoundTag out = new CompoundTag();
        for (Loaded<?> registry : loaded.values()) {
            out.put(registry.key.location().toString(), registry.write());
        }
        return out;
    }

    /** Takes what {@link #write} sent, replacing everything this side held. */
    public synchronized void read(CompoundTag tag) {
        loaded.clear();
        for (Declared<?> declared : DECLARED.values()) {
            String name = declared.key.location().toString();
            loaded.put(declared.key, declared.read(tag.getList(name, NbtTypes.COMPOUND)));
        }
    }

    /**
     * Reads every declared registry from {@code resources} now, as the reload listeners will: for a
     * reader that runs before them in the same reload (the loot tables).
     */
    public synchronized void load(ResourceManager resources) {
        for (Declared<?> declared : DECLARED.values()) {
            put(declared.load(resources));
        }
    }

    /** Forgets everything: a client leaving its server, a server stopping. */
    public synchronized void clear() {
        loaded.clear();
    }

    private synchronized void put(Loaded<?> registry) {
        loaded.put(registry.key, registry);
    }

    @Override
    public String toString() {
        return "DataRegistries[" + side + "]";
    }

    private static final class Declared<T> {
        final ResourceKey<Registry<T>> key;
        final Codec<T> codec;

        Declared(ResourceKey<Registry<T>> key, Codec<T> codec) {
            this.key = key;
            this.codec = codec;
        }

        String folder() {
            return key.location().getNamespace() + "/" + key.location().getPath();
        }

        Loaded<T> empty() {
            return new Loaded<>(key, codec, Collections.emptyMap());
        }

        Loaded<T> load(ResourceManager resources) {
            Map<ResourceLocation, JsonElement> files = new TreeMap<>();
            String folder = folder();
            for (ResourceLocation file : resources.listResources(folder, path -> path.endsWith(".json"))) {
                String path = file.getPath();
                ResourceLocation id = new ResourceLocation(file.getNamespace(),
                        path.substring(folder.length() + 1, path.length() - ".json".length()));
                try (Reader reader = new BufferedReader(new InputStreamReader(
                        resources.getResource(file).getInputStream(), StandardCharsets.UTF_8))) {
                    files.put(id, GsonHelper.fromJson(GSON, reader, JsonElement.class));
                } catch (IOException | RuntimeException broken) {
                    Hempdustry.LOGGER.error("Could not read {} {}: {}", key.location(), id, broken.toString());
                }
            }
            return parse(files);
        }

        Loaded<T> parse(Map<ResourceLocation, JsonElement> files) {
            Map<ResourceLocation, T> values = new TreeMap<>();
            files.forEach((id, json) -> codec.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> Hempdustry.LOGGER.error(
                            "Could not read {} {}: {}", key.location(), id, error))
                    .ifPresent(value -> values.put(id, value)));
            return new Loaded<>(key, codec, values);
        }

        PreparableReloadListener listener() {
            return new SimpleJsonResourceReloadListener(GSON, folder()) {
                @Override
                protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
                                     ProfilerFiller profiler) {
                    SERVER.put(parse(files));
                }
            };
        }

        Loaded<T> read(ListTag entries) {
            Map<ResourceLocation, T> values = new TreeMap<>();
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag entry = entries.getCompound(i);
                ResourceLocation id = new ResourceLocation(entry.getString("id"));
                codec.parse(NbtOps.INSTANCE, entry.get("value"))
                        .resultOrPartial(error -> Hempdustry.LOGGER.error(
                                "The server sent a {} {} this client cannot read: {}", key.location(), id, error))
                        .ifPresent(value -> values.put(id, value));
            }
            return new Loaded<>(key, codec, values);
        }
    }

    private static final class Loaded<T> implements HolderLookup.RegistryLookup<T> {
        final ResourceKey<Registry<T>> key;
        final Codec<T> codec;
        final Map<ResourceKey<T>, Holder.Reference<T>> entries;

        Loaded(ResourceKey<Registry<T>> key, Codec<T> codec, Map<ResourceLocation, T> values) {
            this.key = key;
            this.codec = codec;
            Map<ResourceKey<T>, Holder.Reference<T>> entries = new LinkedHashMap<>();
            values.forEach((id, value) -> {
                ResourceKey<T> entryKey = ResourceKey.create(key, id);
                entries.put(entryKey, new Holder.Reference<>(entryKey, value));
            });
            this.entries = Collections.unmodifiableMap(entries);
        }

        @Override
        public ResourceKey<? extends Registry<? extends T>> key() {
            return key;
        }

        @Override
        public Stream<Holder.Reference<T>> listElements() {
            return entries.values().stream();
        }

        @Override
        public Optional<Holder.Reference<T>> get(ResourceKey<T> entry) {
            return Optional.ofNullable(entries.get(entry));
        }

        ListTag write() {
            ListTag out = new ListTag();
            for (Holder.Reference<T> entry : entries.values()) {
                @Nullable Tag value = codec.encodeStart(NbtOps.INSTANCE, entry.value())
                        .resultOrPartial(error -> Hempdustry.LOGGER.error(
                                "Could not send {} {}: {}", key.location(), entry.key().location(), error))
                        .orElse(null);
                if (value == null) {
                    continue;
                }
                CompoundTag tag = new CompoundTag();
                tag.putString("id", entry.key().location().toString());
                tag.put("value", value);
                out.add(tag);
            }
            return out;
        }
    }
}
