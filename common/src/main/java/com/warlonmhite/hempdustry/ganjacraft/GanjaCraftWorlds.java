package com.warlonmhite.hempdustry.ganjacraft;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.warlonmhite.hempdustry.Hempdustry;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Opens a world played with GanjaCraft, the 1.16.5 mod Hempdustry grew out of, or with Hemp-Craft, its
 * 1.18 successor, as a Hempdustry world.
 *
 * <p>GanjaCraft came as two mods under one id, {@code ganjacraft}: the original, built with MCreator
 * (its BETA 0.4 to 0.5.1), and a later rewrite (1.0.0 to 1.0.6), which share almost no ids. A world
 * may hold either. Hemp-Craft is that rewrite carried on to Minecraft 1.18 under the id
 * {@code hempcraft}, with its ids kept and a villager added. Every id either one saved is in {@code hempdustry/ganjacraft.json}, beside the
 * thing of ours it becomes: a block in a given state, an item, a block entity, an entity, a
 * painting. What GanjaCraft had and Hempdustry has not becomes its nearest equivalent -- the table says
 * which, and why, entry by entry -- so a player loses nothing they had made, though some of it comes
 * back as something else.
 *
 * <p>The conversion happens where the game reads each id from the save, before it looks the id up
 * (mixins in {@code mixin/ganjacraft}): a chunk's block palette, an item stack, a block entity, an
 * entity, a painting's motive. A converted chunk or inventory is written back in Hempdustry's ids the
 * next time the game saves it, so the work is done once and the world needs GanjaCraft for nothing.
 * The loader is told to let the old ids go quietly, rather than warn that the world lost content.
 */
public final class GanjaCraftWorlds {

    /** The two mods' ids: GanjaCraft's, and Hemp-Craft's, which took over from it. */
    public static final Set<String> NAMESPACES = Set.of("ganjacraft", "hempcraft");
    private static final String TABLE = "/hempdustry/ganjacraft.json";

    private static volatile @Nullable Table table;

    private GanjaCraftWorlds() {
    }

    /** Whether an id is one of GanjaCraft's or Hemp-Craft's. */
    public static boolean isGanjaCraft(String id) {
        int colon = id.indexOf(':');
        return colon > 0 && NAMESPACES.contains(id.substring(0, colon));
    }

    /**
     * The state a block GanjaCraft saved -- {@code {Name, Properties}} as a chunk's palette holds it --
     * becomes, or {@code null} for a name that is not GanjaCraft's. An entry the table does not know
     * becomes air, and says so in the log once.
     */
    public static @Nullable BlockState block(CompoundTag saved) {
        String name = saved.getString("Name");
        if (!isGanjaCraft(name)) {
            return null;
        }
        BlockRule rule = table().blocks.get(name);
        if (rule == null) {
            unknown("block", name);
            return Blocks.AIR.defaultBlockState();
        }
        return rule.convert(saved.getCompound("Properties"));
    }

    /**
     * What an item stack GanjaCraft saved -- {@code {id, Count, tag}} -- becomes: a new tag for the
     * game to read, or {@code null} for a stack that is not GanjaCraft's. An item the table maps to
     * nothing, or does not know, becomes an empty stack.
     */
    public static @Nullable CompoundTag item(CompoundTag saved) {
        String id = saved.getString("id");
        if (!isGanjaCraft(id)) {
            return packed(saved);
        }
        ItemRule rule = table().items.get(id);
        if (rule == null) {
            unknown("item", id);
            return emptyStack();
        }
        return rule.convert(saved);
    }

    /**
     * A stack of someone else's that carries GanjaCraft stacks in its tag -- a shulker box picked up
     * full, whose contents are read only once it is placed again -- converted, or {@code null} when it
     * carries none. Converted here, so the box's tooltip and a recipe viewer already see Hempdustry's.
     */
    private static @Nullable CompoundTag packed(CompoundTag saved) {
        CompoundTag tag = saved.getCompound("tag");
        CompoundTag blockEntity = tag.getCompound("BlockEntityTag");
        ListTag inside = blockEntity.getList("Items", 10);
        boolean changed = false;
        ListTag converted = new ListTag();
        for (int i = 0; i < inside.size(); i++) {
            CompoundTag stack = inside.getCompound(i);
            CompoundTag becomes = item(stack);
            if (becomes != null) {
                changed = true;
                if (becomes.getByte("Count") <= 0) {
                    continue;
                }
                if (stack.contains("Slot")) {
                    becomes.putByte("Slot", stack.getByte("Slot"));
                }
                converted.add(becomes);
            } else {
                converted.add(stack);
            }
        }
        if (!changed) {
            return null;
        }
        CompoundTag out = saved.copy();
        out.getCompound("tag").getCompound("BlockEntityTag").put("Items", converted);
        return out;
    }

    /**
     * The block entity type a GanjaCraft block entity becomes: {@code Optional.of(id)} to read it as
     * that type, {@code Optional.empty()} to drop it (its block, converted, needs none), or {@code null}
     * for an id that is not GanjaCraft's.
     */
    public static @Nullable Optional<String> blockEntity(String id) {
        if (!isGanjaCraft(id)) {
            return null;
        }
        Map<String, String> blockEntities = table().blockEntities;
        if (!blockEntities.containsKey(id)) {
            unknown("block entity", id);
            return Optional.empty();
        }
        return Optional.ofNullable(blockEntities.get(id));
    }

    /** The entity type a GanjaCraft entity becomes, as {@link #blockEntity}. */
    public static @Nullable Optional<String> entity(String id) {
        if (!isGanjaCraft(id)) {
            return null;
        }
        Map<String, String> entities = table().entities;
        if (!entities.containsKey(id)) {
            unknown("entity", id);
            return Optional.empty();
        }
        return Optional.ofNullable(entities.get(id));
    }

    /** The painting a GanjaCraft painting's motive becomes, or {@code null} for a motive not GanjaCraft's. */
    public static @Nullable String painting(String motive) {
        if (!isGanjaCraft(motive)) {
            return null;
        }
        String to = table().paintings.get(motive);
        if (to == null) {
            unknown("painting", motive);
            return "minecraft:kebab";
        }
        return to;
    }

    /**
     * The profession a villager of the old mod's becomes, or {@code null} for a profession not its own.
     * A profession the table does not know becomes no profession at all, and the villager finds a job
     * again.
     */
    public static @Nullable String profession(String id) {
        if (!isGanjaCraft(id)) {
            return null;
        }
        String to = table().professions.get(id);
        if (to == null) {
            unknown("profession", id);
            return "minecraft:none";
        }
        return to;
    }

    /**
     * The point of interest type a saved point of the old mod's becomes: {@code Optional.of(id)}, or
     * {@code Optional.empty()} to let the point go (its block, converted, is none any more); {@code null}
     * for a type not the old mod's.
     */
    public static @Nullable Optional<String> pointOfInterest(String id) {
        if (!isGanjaCraft(id)) {
            return null;
        }
        Map<String, String> points = table().pointsOfInterest;
        if (!points.containsKey(id)) {
            unknown("point of interest", id);
            return Optional.empty();
        }
        return Optional.ofNullable(points.get(id));
    }

    private static CompoundTag emptyStack() {
        CompoundTag empty = new CompoundTag();
        empty.putString("id", "minecraft:air");
        empty.putByte("Count", (byte) 0);
        return empty;
    }

    private static final Map<String, Boolean> REPORTED = Collections.synchronizedMap(new HashMap<>());

    private static void unknown(String kind, String id) {
        if (REPORTED.put(kind + " " + id, Boolean.TRUE) == null) {
            Hempdustry.LOGGER.warn("{} {} {} has no Hempdustry equivalent; it is left out of this world",
                    id.startsWith("hempcraft:") ? "Hemp-Craft" : "GanjaCraft", kind, id);
        }
    }

    static Table table() {
        Table loaded = table;
        if (loaded == null) {
            synchronized (GanjaCraftWorlds.class) {
                loaded = table;
                if (loaded == null) {
                    table = loaded = Table.load();
                }
            }
        }
        return loaded;
    }

    /** The table, as {@code hempdustry/ganjacraft.json} writes it. */
    static final class Table {
        final Map<String, BlockRule> blocks = new HashMap<>();
        final Map<String, ItemRule> items = new HashMap<>();
        final Map<String, String> blockEntities = new HashMap<>();
        final Map<String, String> entities = new HashMap<>();
        final Map<String, String> paintings = new HashMap<>();
        final Map<String, String> professions = new HashMap<>();
        final Map<String, String> pointsOfInterest = new HashMap<>();

        static Table load() {
            Table out = new Table();
            try (InputStream in = GanjaCraftWorlds.class.getResourceAsStream(TABLE)) {
                if (in == null) {
                    throw new IllegalStateException("The jar has no " + TABLE);
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                    json.getAsJsonObject("blocks").entrySet().forEach(entry ->
                            out.blocks.put(entry.getKey(), new BlockRule(entry.getValue().getAsJsonObject())));
                    json.getAsJsonObject("items").entrySet().forEach(entry ->
                            out.items.put(entry.getKey(), new ItemRule(entry.getValue().getAsJsonObject())));
                    strings(json, "block_entities", out.blockEntities);
                    strings(json, "entities", out.entities);
                    strings(json, "paintings", out.paintings);
                    strings(json, "professions", out.professions);
                    strings(json, "points_of_interest", out.pointsOfInterest);
                }
            } catch (Exception broken) {
                throw new IllegalStateException("Could not read " + TABLE, broken);
            }
            return out;
        }

        private static void strings(JsonObject json, String key, Map<String, String> into) {
            if (!json.has(key)) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(key).entrySet()) {
                into.put(entry.getKey(), entry.getValue().isJsonNull() ? null : entry.getValue().getAsString());
            }
        }
    }

    /**
     * One GanjaCraft block: the block it becomes ({@code to}), the properties it sets ({@code set}),
     * the old properties it renames ({@code rename}, old name to new) and the values it renames
     * ({@code values}, new property to old value to new value). Every old property the new block has
     * under the same name, and no {@code set} overrides, is carried over as it was -- a stair keeps its
     * facing, a slab its type.
     */
    static final class BlockRule {
        private final String to;
        private final Map<String, String> set = new HashMap<>();
        private final Map<String, String> rename = new HashMap<>();
        private final Map<String, Map<String, String>> values = new HashMap<>();

        BlockRule(JsonObject json) {
            to = json.get("to").getAsString();
            if (json.has("set")) {
                json.getAsJsonObject("set").entrySet().forEach(e -> set.put(e.getKey(), e.getValue().getAsString()));
            }
            if (json.has("rename")) {
                json.getAsJsonObject("rename").entrySet().forEach(e -> rename.put(e.getKey(), e.getValue().getAsString()));
            }
            if (json.has("values")) {
                json.getAsJsonObject("values").entrySet().forEach(property -> {
                    Map<String, String> map = new HashMap<>();
                    property.getValue().getAsJsonObject().entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
                    values.put(property.getKey(), map);
                });
            }
        }

        BlockState convert(CompoundTag oldProperties) {
            Block block = Registry.BLOCK.get(new ResourceLocation(to));
            BlockState state = block.defaultBlockState();
            Map<String, String> wanted = new HashMap<>();
            for (String key : oldProperties.getAllKeys()) {
                String name = rename.getOrDefault(key, key);
                String value = oldProperties.getString(key);
                Map<String, String> byValue = values.get(name);
                wanted.put(name, byValue != null ? byValue.getOrDefault(value, value) : value);
            }
            wanted.putAll(set);
            for (Map.Entry<String, String> entry : wanted.entrySet()) {
                Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
                if (property != null) {
                    state = with(state, property, entry.getValue());
                }
            }
            return state;
        }

        private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String value) {
            return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
        }
    }

    /**
     * One GanjaCraft item: the item it becomes ({@code to}, {@code minecraft:air} for nothing), a tag
     * merged into the stack's ({@code tag}, as SNBT -- a spliff's strain, say), and a count that
     * replaces the stack's ({@code count}) when one GanjaCraft item stands for several of ours. The
     * stack's own tag -- damage, a name, enchantments -- is kept.
     */
    static final class ItemRule {
        private final String to;
        private final @Nullable CompoundTag tag;
        private final int count;

        ItemRule(JsonObject json) {
            to = json.get("to").getAsString();
            count = json.has("count") ? json.get("count").getAsInt() : -1;
            CompoundTag parsed = null;
            if (json.has("tag")) {
                try {
                    parsed = TagParser.parseTag(json.get("tag").getAsString());
                } catch (CommandSyntaxException broken) {
                    throw new IllegalStateException("Bad tag for " + to + ": " + broken.getMessage(), broken);
                }
            }
            tag = parsed;
        }

        CompoundTag convert(CompoundTag saved) {
            if ("minecraft:air".equals(to)) {
                return emptyStack();
            }
            CompoundTag out = saved.copy();
            out.putString("id", to);
            if (count > 0) {
                out.putByte("Count", (byte) Math.min(64, Math.max(1, saved.getByte("Count")) * count));
            }
            if (tag != null) {
                CompoundTag merged = out.contains("tag", 10) ? out.getCompound("tag") : new CompoundTag();
                merged.merge(tag.copy());
                out.put("tag", merged);
            }
            return out;
        }
    }
}
