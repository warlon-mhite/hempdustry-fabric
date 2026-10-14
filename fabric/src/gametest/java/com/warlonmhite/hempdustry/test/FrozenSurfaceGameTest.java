package com.warlonmhite.hempdustry.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagFile;
import net.minecraft.world.Container;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootTable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Every public release is non-breaking: a world, a datapack or an addon that worked on one public
 * build works on the next. These two tests hold the mod to that against what each release actually
 * shipped, instead of against a list someone keeps by hand.
 *
 * <p><b>How a release is frozen.</b> Every run writes what this build freezes to
 * {@code build/gametest/frozen/<version>.txt} (every id, state value, criterion name, asset path and
 * config key — one per line) and {@code <version>.json} (every data file, every stack the creative tab
 * builds, every block entity's save, the default config). Cutting a release copies the two into
 * {@code src/gametest/resources/frozen/}, and from then on every later build is checked against them.
 * 2.0.3's were captured from its release tag; 2.0.2's held nothing 2.0.3's does not, so they are
 * not kept.
 *
 * <p><b>Additions are free; removals fail.</b> Something that has to go anyway goes into
 * {@code frozen/retired.txt} with how old worlds and packs survive it — and a retirement that excuses
 * nothing any more fails as well.
 */
public final class FrozenSurfaceGameTest {

    private static final BlockPos AT = new BlockPos(1, 1, 1);

    /**
     * Every line a released build froze is still true of this one: its items, blocks, entities, block
     * entities, components, sounds and the rest of its registry ids; every value of every blockstate
     * property; every criterion name of every advancement; every recipe, loot table and tag id; every
     * asset path a resource pack can replace; every config key; every strain's {@code model_index}.
     *
     * <p>Trim Season's only criterion was renamed during 2.1 and vanilla drops a criterion an
     * advancement no longer has, so every 2.0.x player who had earned it lost it on joining — found by
     * a review, not a test. The recipe-unlock advancements under {@code recipes/} are left out: one
     * lost there fires again the next time the item is picked up, and the recipe line beside it is
     * what is frozen.
     */
    public static void theFrozenSurfaceHolds(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        SortedSet<String> surface = surface(world);
        String version = version();
        write("frozen/" + version + ".txt", String.join("\n", surface) + "\n");
        write("frozen/" + version + ".json", new GsonBuilder().disableHtmlEscaping().create().toJson(capture(context)));

        Map<String, String> findings = new TreeMap<>();
        int baselines = 0;
        for (Map.Entry<String, Path> file : frozenFiles().entrySet()) {
            if (!file.getKey().endsWith(".txt") || file.getKey().equals("retired.txt")) continue;
            baselines++;
            for (String line : text(file.getValue()).lines().filter(line -> !line.isBlank()).toList()) {
                if (!surface.contains(line)) findings.putIfAbsent(line, "frozen by " + file.getKey() + ", gone from this build");
            }
        }
        Sweep.atLeast(context, "release baselines in frozen/", baselines, 1);
        Sweep.atLeast(context, "frozen lines in this build", surface.size(), 1000);
        Sweep.report(context, "Everything a public release froze is still here", findings, retired());
        context.succeed();
    }

    /**
     * Every data file a release shipped still decodes with this build's codecs — so a datapack that
     * copied one and changed it still loads — and every stack, block entity save and config file a
     * release wrote reads back with nothing it held lost.
     *
     * <p>A codec field that turns from optional to required, a serializer that renames a field, a
     * block entity that stops reading a key: each makes a world or a pack written for the release
     * fail to load or quietly lose data, and nothing in the build itself notices. This is the cheap,
     * every-run half of the upgrade play-test, which stays the end-to-end proof at a release.
     */
    public static void oldDataStillLoads(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        RegistryAccess registries = world.registryAccess();
        DynamicOps<JsonElement> json = registries.createSerializationContext(JsonOps.INSTANCE);
        Map<String, String> findings = new TreeMap<>();
        int files = 0;
        int releases = 0;
        for (Map.Entry<String, Path> file : frozenFiles().entrySet()) {
            if (!file.getKey().endsWith(".json")) continue;
            releases++;
            String release = file.getKey().replace(".json", "");
            JsonObject old = Sweep.json(file.getValue()).getAsJsonObject();

            for (Map.Entry<String, JsonElement> data : old.getAsJsonObject("data").entrySet()) {
                files++;
                Codec<?> codec = codecFor(data.getKey());
                if (codec == null) {
                    findings.put(release + " " + data.getKey(), "the test cannot tell what this file is: teach codecFor() its folder");
                    continue;
                }
                DataResult<?> decoded = codec.parse(json, data.getValue());
                if (decoded.isError()) {
                    findings.put(release + " " + data.getKey(), "no longer decodes: " + decoded.error().orElseThrow().message());
                }
            }

            for (JsonElement saved : old.getAsJsonArray("stacks")) {
                DataResult<ItemStack> stack = ItemStack.CODEC.parse(json, saved);
                String key = release + " stack " + saved.getAsJsonObject().get("id").getAsString();
                if (stack.isError()) {
                    findings.putIfAbsent(key, "no longer loads: " + stack.error().orElseThrow().message() + " — " + saved);
                } else if (!contains(ItemStack.CODEC.encodeStart(json, stack.getOrThrow()).getOrThrow(), saved)) {
                    findings.putIfAbsent(key, "loads as " + ItemStack.CODEC.encodeStart(json, stack.getOrThrow()).getOrThrow()
                            + ", losing part of " + saved);
                }
            }

            BlockPos pos = context.absolutePos(AT);
            for (Map.Entry<String, JsonElement> save : old.getAsJsonObject("blockEntities").entrySet()) {
                String key = release + " " + save.getKey();
                Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(save.getKey())).orElse(null);
                if (block == null) {
                    findings.put(key, "the block is gone");
                    continue;
                }
                CompoundTag before = nbt(save.getValue().getAsString());
                BlockEntity loaded = BlockEntity.loadStatic(pos, block.defaultBlockState(), before.copy(), registries);
                if (loaded == null) {
                    findings.put(key, "its block entity no longer loads from " + before);
                } else if (!contains(placeless(loaded.saveWithFullMetadata(registries)), placeless(before))) {
                    findings.put(key, "saved " + before + " and loads as " + loaded.saveWithFullMetadata(registries));
                }
            }

            // The file as the release wrote it, every key in it: each must still be read and kept.
            JsonElement config = old.get("config");
            ConfigGameTest.withConfig(config.toString(), () -> {
                JsonElement now = HempdustryConfig.CODEC.encodeStart(JsonOps.INSTANCE, HempdustryConfig.get()).getOrThrow();
                if (!contains(now, config)) {
                    findings.put(release + " config", "a config file it wrote now reads as " + now + ", not " + config);
                }
            });
        }
        Sweep.atLeast(context, "released data files", files, 300);
        Sweep.atLeast(context, "releases in frozen/", releases, 1);
        Sweep.report(context, "What a public release wrote still loads", findings, Map.of());
        context.succeed();
    }

    // ----- what this build freezes -----

    private static SortedSet<String> surface(ServerLevel world) {
        RegistryAccess registries = world.registryAccess();
        SortedSet<String> lines = new TreeSet<>();
        registries.registries().forEach(entry -> {
            Identifier registry = entry.key().identifier();
            if (Sweep.ours(registry)) lines.add("registry " + registry);
            for (Identifier id : entry.value().keySet()) {
                if (Sweep.ours(id)) lines.add("entry " + registry + " " + id);
            }
            entry.value().getTags().forEach(tag -> {
                Identifier id = tag.key().location();
                // Ours, and the c: subtags we mint; a tag we only add to is vanilla's or another mod's.
                if (Sweep.ours(id) || id.getNamespace().equals("c") && id.getPath().endsWith("/hemp")) {
                    lines.add("tag " + registry + " #" + id);
                }
            });
        });
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!Sweep.ours(id)) continue;
            for (Property<?> property : block.getStateDefinition().getProperties()) states(id, property, lines);
        }
        for (AdvancementHolder advancement : world.getServer().getAdvancements().getAllAdvancements()) {
            Identifier id = advancement.id();
            if (!Sweep.ours(id) || id.getPath().startsWith("recipes/")) continue;
            for (String criterion : advancement.value().criteria().keySet()) lines.add("criterion " + id + " " + criterion);
        }
        for (RecipeHolder<?> recipe : world.getServer().getRecipeManager().getRecipes()) {
            if (Sweep.ours(recipe.id().identifier())) lines.add("recipe " + recipe.id().identifier());
        }
        world.getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE)
                .listElementIds().filter(key -> Sweep.ours(key.identifier()))
                .forEach(key -> lines.add("loot_table " + key.identifier()));
        for (String file : Sweep.modFiles().keySet()) {
            if (file.startsWith("assets/" + Hempdustry.MOD_ID + "/") && !file.startsWith("assets/" + Hempdustry.MOD_ID + "/lang/")) {
                lines.add("asset " + file);
            }
        }
        keys("", HempdustryConfig.CODEC.encodeStart(JsonOps.INSTANCE, HempdustryConfig.DEFAULT).getOrThrow(), lines);
        registries.lookupOrThrow(Strain.REGISTRY_KEY).entrySet().forEach(strain -> {
            if (Sweep.ours(strain.getKey().identifier())) {
                lines.add("model_index " + strain.getKey().identifier() + " " + strain.getValue().modelIndex());
            }
        });
        return lines;
    }

    private static <T extends Comparable<T>> void states(Identifier block, Property<T> property, SortedSet<String> into) {
        for (T value : property.getPossibleValues()) into.add("state " + block + " " + property.getName() + "=" + property.getName(value));
    }

    private static void keys(String prefix, JsonElement json, SortedSet<String> into) {
        if (json.isJsonObject()) {
            json.getAsJsonObject().entrySet().forEach(e -> keys(prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey(), e.getValue(), into));
        } else {
            into.add("config " + prefix);
        }
    }

    /**
     * What this build wrote, for {@link #oldDataStillLoads} to read back once it is a release: every
     * data file it ships, every stack its creative tab builds, a filled and named block entity of
     * every block that has one, and its default config.
     */
    private static JsonObject capture(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        RegistryAccess registries = world.registryAccess();
        JsonObject out = new JsonObject();

        JsonObject data = new JsonObject();
        Sweep.modFiles().forEach((file, path) -> {
            if (file.startsWith("data/") && file.endsWith(".json")) data.add(file, Sweep.json(path));
        });
        out.add("data", data);

        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.buildContents(new CreativeModeTab.ItemDisplayParameters(
                world.enabledFeatures(), true, registries));
        JsonArray stacks = new JsonArray();
        Stream.concat(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayItems().stream(),
                        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabDisplayItems().stream())
                .map(stack -> ItemStack.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow())
                .distinct().forEach(stacks::add);
        out.add("stacks", stacks);

        JsonObject blockEntities = new JsonObject();
        BlockPos pos = context.absolutePos(AT);
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            BlockState state = block.defaultBlockState();
            if (!Sweep.ours(id) || !state.hasBlockEntity()) continue;
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity == null) continue;
            if (entity instanceof Container inventory) {
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) inventory.setItem(slot, new ItemStack(Items.DIAMOND, slot + 1));
            }
            ItemStack named = new ItemStack(block.asItem());
            if (!named.isEmpty()) {
                named.set(DataComponents.CUSTOM_NAME, Component.literal("Frozen"));
                entity.applyComponentsFromItemStack(named);
            }
            blockEntities.addProperty(id.toString(), entity.saveWithFullMetadata(registries).toString());
            if (entity instanceof Container inventory) inventory.clearContent();
            world.removeBlock(pos, false);
        }
        out.add("blockEntities", blockEntities);

        out.add("config", HempdustryConfig.CODEC.encodeStart(JsonOps.INSTANCE, HempdustryConfig.DEFAULT).getOrThrow());
        return out;
    }

    // ----- reading what a release froze -----

    /** The codec a data file at {@code path} is read with, by its folder; null for a folder nobody taught it. */
    private static Codec<?> codecFor(String path) {
        String inside = path.substring(path.indexOf('/', "data/".length()) + 1);
        if (inside.startsWith("tags/")) return TagFile.CODEC;
        if (inside.startsWith("recipe/")) return Recipe.CODEC;
        if (inside.startsWith("advancement/")) return Advancement.CODEC;
        if (inside.startsWith("loot_table/")) return LootTable.DIRECT_CODEC;
        // Fabric's list, not vanilla's: a mod's dynamic registry is in Fabric's alone. Its files live
        // under <namespace>/<path>, data/<pack>/hempdustry/strain/ for strains.
        for (RegistryDataLoader.RegistryData<?> registry : DynamicRegistries.getDynamicRegistries()) {
            Identifier id = registry.key().identifier();
            String folder = id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? id.getPath() : id.getNamespace() + "/" + id.getPath();
            if (inside.startsWith(folder + "/")) return registry.elementCodec();
        }
        return null;
    }

    /** Whether {@code now} still holds everything {@code then} held: every key of every object, every other value equal. */
    private static boolean contains(JsonElement now, JsonElement then) {
        if (then.isJsonObject() && now.isJsonObject()) {
            JsonObject object = now.getAsJsonObject();
            return then.getAsJsonObject().entrySet().stream().allMatch(
                    e -> object.has(e.getKey()) && contains(object.get(e.getKey()), e.getValue()));
        }
        return now.equals(then);
    }

    private static boolean contains(CompoundTag now, CompoundTag then) {
        for (String key : then.keySet()) {
            Tag had = then.get(key);
            Tag has = now.get(key);
            if (has == null) return false;
            if (had instanceof CompoundTag hadCompound && has instanceof CompoundTag hasCompound) {
                if (!contains(hasCompound, hadCompound)) return false;
            } else if (!had.equals(has)) {
                return false;
            }
        }
        return true;
    }

    /** A save without its position, so one loaded somewhere else compares equal. */
    private static CompoundTag placeless(CompoundTag save) {
        CompoundTag copy = save.copy();
        copy.remove("x");
        copy.remove("y");
        copy.remove("z");
        return copy;
    }

    /** {@code frozen/retired.txt}: a line each, then two spaces, a {@code #} and how old worlds survive it. */
    private static Map<String, String> retired() {
        Map<String, String> retired = new TreeMap<>();
        Path file = frozenFiles().get("retired.txt");
        if (file == null) return retired;
        for (String line : text(file).lines().toList()) {
            int reason = line.indexOf("  # ");
            if (!line.isBlank() && !line.startsWith("#")) {
                retired.put(reason < 0 ? line.strip() : line.substring(0, reason).strip(),
                        reason < 0 ? "(no reason given)" : line.substring(reason + 4).strip());
            }
        }
        return retired;
    }

    /** The release baselines this test mod ships, by name inside {@code frozen/}. */
    private static Map<String, Path> frozenFiles() {
        Map<String, Path> files = new TreeMap<>();
        for (Path root : FabricLoader.getInstance().getModContainer(HempdustryGameTests.NAMESPACE).orElseThrow().getRootPaths()) {
            Path frozen = root.resolve("frozen");
            if (!Files.isDirectory(frozen)) continue;
            try (Stream<Path> walk = Files.list(frozen)) {
                walk.filter(Files::isRegularFile).forEach(file -> files.put(file.getFileName().toString(), file));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return files;
    }

    /** The release this build would be: {@code 2.1.0-beta} out of {@code 2.1.0-beta+1.21.11}. */
    private static String version() {
        String full = FabricLoader.getInstance().getModContainer(Hempdustry.MOD_ID).orElseThrow()
                .getMetadata().getVersion().getFriendlyString();
        return full.contains("+") ? full.substring(0, full.indexOf('+')) : full;
    }

    private static void write(String file, String content) {
        try {
            Path path = Path.of(file);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String text(Path file) {
        return new String(Sweep.bytes(file), StandardCharsets.UTF_8);
    }

    private static CompoundTag nbt(String snbt) {
        try {
            return TagParser.parseCompoundFully(snbt);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
