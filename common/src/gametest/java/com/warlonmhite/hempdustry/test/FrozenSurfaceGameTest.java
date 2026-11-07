package com.warlonmhite.hempdustry.test;

import net.minecraft.network.chat.TextComponent;
import com.warlonmhite.hempdustry.util.BlockFlags;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Paths;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTables;
import com.warlonmhite.hempdustry.registry.DataRegistries;
import com.warlonmhite.hempdustry.registry.HolderLookup;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceKey;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.advancements.critereon.DeserializationContext;
import com.warlonmhite.hempdustry.util.SemVer;
import com.warlonmhite.hempdustry.block.entity.NamedMachineBlockEntity;
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
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.Tag.Builder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
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
 * This line has had no release yet, so {@code frozen/} holds none of its own: its first release
 * starts it.
 *
 * <p><b>A world made on this line moves to a newer Minecraft, never the other way.</b> So
 * {@code frozen/1.21.1/} holds the 1.21.1 line's releases, and {@link #aWorldFromThisLineUpgrades}
 * holds this build to the newest of them: everything a 1.16.5 world can have saved must be there
 * after Minecraft's upgrade to 1.21.1, under the newer names of the registries Minecraft has renamed
 * since ({@link #RENAMED_REGISTRIES}).
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
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void theFrozenSurfaceHolds(GameTestHelper context) {
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
            for (String line : lines(file.getValue())) {
                if (!surface.contains(line)) findings.putIfAbsent(line, "frozen by " + file.getKey() + ", gone from this build");
            }
        }
        // None until this line's first release (see the class comment).
        Sweep.atLeast(context, "release baselines in frozen/", baselines, 0);
        Sweep.atLeast(context, "frozen lines in this build", surface.size(), 1000);
        Sweep.report(context, "Everything a public release froze is still here", findings, retired());
        context.succeed();
    }

    /**
     * A world made on this line upgrades to the 1.21.1 line with nothing of the mod's lost: every
     * registry id, state value, advancement criterion, recipe, loot table, config key and strain
     * {@code model_index} this build has is in the newest 1.21.1 release's surface
     * ({@code frozen/1.21.1/}). What a save cannot hold -- tags, assets, the recipe types a datapack
     * names -- is free to differ, as it is between any two lines.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aWorldFromThisLineUpgrades(GameTestHelper context) {
        Map<String, Path> newer = frozenFiles("frozen/1.21.1");
        String newest = newer.keySet().stream().filter(name -> name.endsWith(".txt") && !name.equals("retired.txt"))
                .max(Comparator.comparing(name -> SemVer.parse(name.substring(0, name.length() - ".txt".length()))))
                .orElse(null);
        context.assertTrue(newest != null, "frozen/1.21.1/ holds no release of the 1.21.1 line");
        Set<String> there = new HashSet<>(lines(newer.get(newest)));
        Map<String, String> findings = new TreeMap<>();
        int checked = 0;
        for (String line : surface(context.getLevel())) {
            if (line.startsWith("tag ") || line.startsWith("asset ") || line.startsWith("registry ")) continue;
            // No save names a feature: 1.16.5 has no placed features, so its decorated features hold the
            // newer lines' placed ids as configured ones, and a 1.16.5-only feature type places Beldia.
            if (line.startsWith("entry minecraft:worldgen/configured_feature ")
                    || line.startsWith("entry minecraft:worldgen/feature ")
                    || line.startsWith("entry minecraft:worldgen/decorator ")) continue;
            line = renamed(line);
            // A loader's own data registry (Forge's biome modifiers) is that loader's to read, from data
            // files a world does not save; the baseline is written by Fabric's run.
            if (line.startsWith("entry forge:") || line.startsWith("entry neoforge:")) continue;
            checked++;
            if (!there.contains(line)) findings.put(line, "not in the 1.21.1 line's " + newest + ": a world upgraded there loses it");
        }
        Sweep.atLeast(context, "lines a 1.16.5 world can hold", checked, 900);
        Sweep.report(context, "A world from this line upgrades to the 1.21.1 line", findings, UPGRADE_EXEMPT);
        context.succeed();
    }

    /** What this line has and the 1.21.1 line has not, each with why a world upgraded there keeps working. */
    private static final Map<String, String> UPGRADE_EXEMPT = ImmutableMap.of(
            "entry minecraft:recipe_serializer hempdustry:crafting_shaped",
            "this line's crafting with a result that carries data, named only by recipe files; no save holds a recipe serializer, and the newer lines' recipes are their own vanilla ones",
            "entry minecraft:recipe_serializer hempdustry:crafting_shapeless",
            "the same, shapeless");

    /**
     * Registries Minecraft renamed between 1.16.5 and 1.21.1, by their 1.16.5 name: a world's ids move
     * with them as it upgrades (vanilla's data fixer turns a painting's {@code Motive} into its
     * {@code variant}), so a 1.16.5 line is looked for under the newer name.
     */
    private static final Map<String, String> RENAMED_REGISTRIES = ImmutableMap.of(
            "minecraft:motive", "minecraft:painting_variant");

    private static String renamed(String line) {
        for (Map.Entry<String, String> rename : RENAMED_REGISTRIES.entrySet()) {
            if (line.startsWith("entry " + rename.getKey() + " ")) {
                return "entry " + rename.getValue() + line.substring(("entry " + rename.getKey()).length());
            }
        }
        return line;
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
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void oldDataStillLoads(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        DynamicOps<JsonElement> json = JsonOps.INSTANCE;
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
                String problem = decodeProblem(world, json, data.getKey(), data.getValue());
                if (problem != null) {
                    findings.put(release + " " + data.getKey(), problem);
                }
            }

            for (JsonElement saved : old.getAsJsonArray("stacks")) {
                DataResult<ItemStack> stack = ItemStack.CODEC.parse(json, saved);
                String key = release + " stack " + saved.getAsJsonObject().get("id").getAsString();
                if (stack.error().isPresent()) {
                    findings.putIfAbsent(key, "no longer loads: " + stack.error().get().message() + " — " + saved);
                } else if (!contains(orThrow(ItemStack.CODEC.encodeStart(json, orThrow(stack))), saved)) {
                    findings.putIfAbsent(key, "loads as " + orThrow(ItemStack.CODEC.encodeStart(json, orThrow(stack)))
                            + ", losing part of " + saved);
                }
            }

            BlockPos pos = context.absolutePos(AT);
            for (Map.Entry<String, JsonElement> save : old.getAsJsonObject("blockEntities").entrySet()) {
                String key = release + " " + save.getKey();
                Block block = Registry.BLOCK.getOptional(new ResourceLocation(save.getKey())).orElse(null);
                if (block == null) {
                    findings.put(key, "the block is gone");
                    continue;
                }
                CompoundTag before = nbt(save.getValue().getAsString());
                BlockEntity loaded = BlockEntity.loadStatic(block.defaultBlockState(), before.copy());
                if (loaded == null) {
                    findings.put(key, "its block entity no longer loads from " + before);
                } else if (!contains(placeless(loaded.save(new CompoundTag())), placeless(before))) {
                    findings.put(key, "saved " + before + " and loads as " + loaded.save(new CompoundTag()));
                }
            }

            // The file as the release wrote it, every key in it: each must still be read and kept.
            JsonElement config = old.get("config");
            ConfigGameTest.withConfig(config.toString(), () -> {
                JsonElement now = orThrow(HempdustryConfig.CODEC.encodeStart(JsonOps.INSTANCE, HempdustryConfig.get()));
                if (!contains(now, config)) {
                    findings.put(release + " config", "a config file it wrote now reads as " + now + ", not " + config);
                }
            });
        }
        // None until this line's first release (see the class comment).
        Sweep.atLeast(context, "released data files", files, 0);
        Sweep.atLeast(context, "releases in frozen/", releases, 0);
        Sweep.report(context, "What a public release wrote still loads", findings, ImmutableMap.of());
        context.succeed();
    }

    // ----- what this build freezes -----

    /**
     * The worldgen registries a 1.16.5 world holds, beside the built-in ones of {@code Registry.REGISTRY}:
     * 1.16.5 lists them in no registry of registries a test can walk.
     */
    private static final List<ResourceKey<? extends Registry<?>>> WORLDGEN = ImmutableList.of(
            Registry.CONFIGURED_FEATURE_REGISTRY, Registry.CONFIGURED_CARVER_REGISTRY,
            Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, Registry.CONFIGURED_SURFACE_BUILDER_REGISTRY,
            Registry.PROCESSOR_LIST_REGISTRY, Registry.TEMPLATE_POOL_REGISTRY, Registry.BIOME_REGISTRY,
            Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, Registry.DIMENSION_TYPE_REGISTRY);

    private static SortedSet<String> surface(ServerLevel world) {
        RegistryAccess registries = world.registryAccess();
        SortedSet<String> lines = new TreeSet<>();
        for (ResourceLocation registry : Registry.REGISTRY.keySet()) {
            entries(registry, Registry.REGISTRY.get(registry).keySet(), lines);
        }
        for (ResourceKey<? extends Registry<?>> key : WORLDGEN) {
            registryIn(registries, key).ifPresent(registry -> entries(key.location(), registry.keySet(), lines));
        }
        // The mod's own datapack registries (DataRegistries): 1.16.5 has none of vanilla's a mod can add.
        HolderLookup.Provider data = DataRegistries.of(world);
        for (TestPlatform.RegistryData<?> registry : TestPlatform.dynamicRegistries()) {
            entries(registry.key().location(), data.lookupOrThrow(registry.key()).listElements()
                    .map(element -> element.key().location()).collect(Collectors.toSet()), lines);
        }
        // 1.16.5 keeps tags in a container of four kinds, not on each registry.
        TagContainer tags = world.getServer().getTags();
        tags(Registry.BLOCK_REGISTRY.location(), tags.getBlocks().getAvailableTags(), lines);
        tags(Registry.ITEM_REGISTRY.location(), tags.getItems().getAvailableTags(), lines);
        tags(Registry.FLUID_REGISTRY.location(), tags.getFluids().getAvailableTags(), lines);
        tags(Registry.ENTITY_TYPE_REGISTRY.location(), tags.getEntityTypes().getAvailableTags(), lines);
        for (Block block : Registry.BLOCK) {
            ResourceLocation id = Registry.BLOCK.getKey(block);
            if (!Sweep.ours(id)) continue;
            for (Property<?> property : block.getStateDefinition().getProperties()) states(id, property, lines);
        }
        for (Advancement advancement : world.getServer().getAdvancements().getAllAdvancements()) {
            ResourceLocation id = advancement.getId();
            if (!Sweep.ours(id) || id.getPath().startsWith("recipes/")) continue;
            for (String criterion : advancement.getCriteria().keySet()) lines.add("criterion " + id + " " + criterion);
        }
        for (Recipe<?> recipe : world.getServer().getRecipeManager().getRecipes()) {
            if (Sweep.ours(recipe.getId())) lines.add("recipe " + recipe.getId());
        }
        world.getServer().getLootTables().getIds().stream()
                .filter(Sweep::ours).forEach(id -> lines.add("loot_table " + id));
        for (String file : Sweep.modFiles().keySet()) {
            if (file.startsWith("assets/" + Hempdustry.MOD_ID + "/") && !file.startsWith("assets/" + Hempdustry.MOD_ID + "/lang/")) {
                lines.add("asset " + file);
            }
        }
        keys("", orThrow(HempdustryConfig.CODEC.encodeStart(JsonOps.INSTANCE, HempdustryConfig.DEFAULT)), lines);
        data.lookupOrThrow(Strain.REGISTRY_KEY).listElements().forEach(strain -> {
            if (Sweep.ours(strain.key().location())) {
                lines.add("model_index " + strain.key().location() + " " + strain.value().modelIndex());
            }
        });
        // A player's save keeps what the mod attaches to them under the attachment's id, which no
        // registry lists on 1.16.5. So it is named here.
        lines.add("attachment " + EdibleScheduler.PENDING_ID);
        return lines;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static java.util.Optional<? extends Registry<?>> registryIn(RegistryAccess registries, ResourceKey<? extends Registry<?>> key) {
        return registries.registry((ResourceKey) key);
    }

    private static void entries(ResourceLocation registry, Set<ResourceLocation> ids, SortedSet<String> into) {
        if (Sweep.ours(registry)) into.add("registry " + registry);
        for (ResourceLocation id : ids) {
            if (Sweep.ours(id)) into.add("entry " + registry + " " + id);
        }
    }

    private static void tags(ResourceLocation registry, java.util.Collection<ResourceLocation> ids, SortedSet<String> into) {
        for (ResourceLocation id : ids) {
            // Ours, and the c: and forge: subtags we mint; a tag we only add to is vanilla's or another mod's.
            if (Sweep.ours(id) || (id.getNamespace().equals("c") || id.getNamespace().equals("forge"))
                    && id.getPath().endsWith("/hemp")) {
                into.add("tag " + registry + " #" + id);
            }
        }
    }

    private static <T extends Comparable<T>> void states(ResourceLocation block, Property<T> property, SortedSet<String> into) {
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
        JsonObject out = new JsonObject();

        JsonObject data = new JsonObject();
        Sweep.modFiles().forEach((file, path) -> {
            if (file.startsWith("data/") && file.endsWith(".json")) data.add(file, Sweep.json(path));
        });
        out.add("data", data);

        JsonArray stacks = new JsonArray();
        Stream.concat(CreativeTab.tab().stream(), CreativeTab.search().stream())
                .map(stack -> orThrow(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, stack)))
                .distinct().forEach(stack -> stacks.add(stack));
        out.add("stacks", stacks);

        JsonObject blockEntities = new JsonObject();
        BlockPos pos = context.absolutePos(AT);
        for (Block block : Registry.BLOCK) {
            ResourceLocation id = Registry.BLOCK.getKey(block);
            BlockState state = block.defaultBlockState();
            if (!Sweep.ours(id) || !block.isEntityBlock()) continue;
            world.setBlock(pos, state, BlockFlags.UPDATE_CLIENTS);
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity == null) continue;
            if (entity instanceof Container inventory) {
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) inventory.setItem(slot, new ItemStack(Items.DIAMOND, slot + 1));
            }
            ItemStack named = new ItemStack(block.asItem());
            // Named as an anvil's name reaches each kind: the machines' own name, a vanilla container's.
            if (entity instanceof NamedMachineBlockEntity machine) {
                machine.setCustomName(new TextComponent("Frozen"));
            } else if (entity instanceof BaseContainerBlockEntity container) {
                container.setCustomName(new TextComponent("Frozen"));
            }
            blockEntities.addProperty(id.toString(), entity.save(new CompoundTag()).toString());
            if (entity instanceof Container inventory) inventory.clearContent();
            world.removeBlock(pos, false);
        }
        out.add("blockEntities", blockEntities);

        out.add("config", orThrow(HempdustryConfig.CODEC.encodeStart(JsonOps.INSTANCE, HempdustryConfig.DEFAULT)));
        return out;
    }

    // ----- reading what a release froze -----

    /**
     * Why the data file at {@code path} no longer reads, or {@code null} when it does. Recipes,
     * advancements, loot tables and tags are read on 1.16.5 as the game reads them, from JSON; the
     * rest by their codecs, found by folder.
     */
    private static String decodeProblem(ServerLevel world, DynamicOps<JsonElement> json, String path, JsonElement file) {
        String inside = path.substring(path.indexOf('/', "data/".length()) + 1);
        String namespace = path.substring("data/".length(), path.indexOf('/', "data/".length()));
        try {
            if (inside.startsWith("recipes/")) {
                RecipeManager.fromJson(id(namespace, inside, "recipes/"), file.getAsJsonObject());
                return null;
            }
            if (inside.startsWith("advancements/")) {
                Advancement.Builder.fromJson(file.getAsJsonObject(), new DeserializationContext(
                        id(namespace, inside, "advancements/"), world.getServer().getPredicateManager()));
                return null;
            }
            if (inside.startsWith("loot_tables/")) {
                return Deserializers.createLootTableSerializer().create().fromJson(file, LootTable.class) != null
                        ? null : "no longer decodes";
            }
            if (inside.startsWith("tags/")) {
                Builder.tag().addFromJson(file.getAsJsonObject(), "frozen");
                return null;
            }
        } catch (RuntimeException e) {
            return "no longer decodes: " + e.getMessage();
        }
        Codec<?> codec = codecFor(inside);
        if (codec == null) {
            return "the test cannot tell what this file is: teach codecFor() its folder";
        }
        DataResult<?> decoded = codec.parse(json, file);
        return decoded.error().map(error -> "no longer decodes: " + error.message()).orElse(null);
    }

    private static ResourceLocation id(String namespace, String inside, String folder) {
        return new ResourceLocation(namespace, inside.substring(folder.length(), inside.length() - ".json".length()));
    }

    /** The codec a data file is read with, by its folder inside its namespace; null for a folder nobody taught it. */
    private static Codec<?> codecFor(String inside) {
        // The mod's own datapack registries (DataRegistries). Their files live under
        // <namespace>/<path>, data/<pack>/hempdustry/strain/ for strains.
        for (TestPlatform.RegistryData<?> registry : TestPlatform.dynamicRegistries()) {
            ResourceLocation id = registry.key().location();
            String folder = id.getNamespace().equals("minecraft") ? id.getPath() : id.getNamespace() + "/" + id.getPath();
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
        for (String key : then.getAllKeys()) {
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
        for (String line : text(file).split("\n")) {
            int reason = line.indexOf("  # ");
            if (!line.trim().isEmpty() && !line.startsWith("#")) {
                retired.put(reason < 0 ? line.trim() : line.substring(0, reason).trim(),
                        reason < 0 ? "(no reason given)" : line.substring(reason + 4).trim());
            }
        }
        return retired;
    }

    private static <T> T orThrow(DataResult<T> result) {
        return result.getOrThrow(false, error -> { });
    }

    /** The release baselines this test mod ships, by name inside {@code frozen/}. */
    private static Map<String, Path> frozenFiles() {
        return frozenFiles("frozen");
    }

    /** The baselines in one folder of the test mod, by name. */
    private static Map<String, Path> frozenFiles(String folder) {
        Map<String, Path> files = new TreeMap<>();
        for (Path root : TestPlatform.INSTANCE.testModRoots()) {
            Path frozen = root.resolve(folder);
            if (!Files.isDirectory(frozen)) continue;
            try (Stream<Path> walk = Files.list(frozen)) {
                walk.filter(Files::isRegularFile).forEach(file -> files.put(file.getFileName().toString(), file));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return files;
    }

    /** Every line of a baseline that is not blank. */
    private static List<String> lines(Path file) {
        return Arrays.stream(text(file).split("\n")).map(String::trim).filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    /** The release this build would be: {@code 2.1.0-beta} out of {@code 2.1.0-beta+1.21.11}. */
    private static String version() {
        String full = Services.PLATFORM.modVersion();
        return full.contains("+") ? full.substring(0, full.indexOf('+')) : full;
    }

    private static void write(String file, String content) {
        try {
            Path path = Paths.get(file);
            Files.createDirectories(path.getParent());
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String text(Path file) {
        return new String(Sweep.bytes(file), StandardCharsets.UTF_8);
    }

    private static CompoundTag nbt(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
