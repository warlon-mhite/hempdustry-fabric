package com.warlonmhite.hempdustry.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.warlonmhite.hempdustry.block.entity.custom.BongBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Every entity and block entity of ours must be one vanilla's data fixer knows, or a world moved
 * across a Minecraft version keeps old data where the new game reads new — and loses it.
 *
 * <p>An entity chunk as 1.21.1 saved it, run through the real fixer to this version: every entity
 * type of ours, each carrying an enchanted sword, beside a vanilla armour stand wearing boots. Before
 * the boats were registered with the fixer ({@code Schema1460Mixin}) it could not read the chunk's
 * entity list at all and passed it through untouched, so the stand never got the {@code equipment}
 * map this version reads its armour from — in a real world, it came up bare. The sword is the chest
 * boat's half: its enchantments changed shape on the way (1.21.5 dropped the {@code levels} wrapper),
 * and only an entity whose {@code Items} the fixer knows about gets them converted.
 *
 * <p>A world from the mod's 1.20.1 build comes from further back, from before items had components:
 * the last two tests run a 1.20.1 stack and a 1.20.1 block chunk through the same fixer. Measured
 * with such a world opened on the 1.21.1 build before the fix there: every value of ours on a stack
 * sat in {@code minecraft:custom_data} (a packed pipe came up unpacked), and a placed bong lost its
 * name, damage, enchantment and load.
 *
 * <p>Both chunks are built from the registries rather than from a list, so a new entity or block
 * entity is checked the day it registers, before anyone remembers the mixin.
 */
public final class DataFixerGameTest {

    /** 1.21.1's data version: what a world coming across from the 1.21.1 line was saved as. */
    private static final int MINECRAFT_1_21_1 = 3955;

    private static final String SWORD = "{Slot:0b,id:\"minecraft:iron_sword\",count:1,"
            + "components:{\"minecraft:enchantments\":{levels:{\"minecraft:sharpness\":3}}}}";

    public static void aHempBoatDoesNotStopTheDataFixer(GameTestHelper context) {
        List<Identifier> ours = new ArrayList<>();
        Map<Identifier, Boolean> holdsItems = new HashMap<>();
        StringBuilder chunk = new StringBuilder("{DataVersion:3955,Position:[I;0,0],Entities:[");
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!Sweep.ours(id)) continue;
            Entity entity = type.create(context.getLevel(), EntitySpawnReason.LOAD);
            holdsItems.put(id, entity instanceof Container);
            chunk.append("{id:\"").append(id).append("\",Pos:[").append(ours.size() * 3).append(".5d,-60.0d,0.5d],")
                    .append("Type:\"oak\",Items:[").append(SWORD).append("]},");
            ours.add(id);
        }
        chunk.append("{id:\"minecraft:armor_stand\",Pos:[-3.5d,-60.0d,0.5d],"
                + "ArmorItems:[{id:\"minecraft:leather_boots\",count:1},{},{},{}],HandItems:[{},{}]}]}");
        Sweep.atLeast(context, "entity types of ours", ours.size(), 2);

        ListTag entities = DataFixTypes.ENTITY_CHUNK.updateToCurrentVersion(
                context.getLevel().getServer().getFixerUpper(), read(chunk.toString()), MINECRAFT_1_21_1)
                .getListOrEmpty("Entities");

        // The fixer writes the new equipment map and leaves the old key beside it; the game drops the
        // old one the next time it saves the entity. The new one is what 1.21.11 reads.
        CompoundTag stand = entities.getCompoundOrEmpty(ours.size());
        context.assertTrue("minecraft:leather_boots".equals(
                        stand.getCompoundOrEmpty("equipment").getCompoundOrEmpty("feet").getStringOr("id", "")),
                "the armour stand beside " + ours + " was not upgraded, so it has no equipment: " + stand);

        Map<String, String> findings = new TreeMap<>();
        for (int i = 0; i < ours.size(); i++) {
            CompoundTag entity = entities.getCompoundOrEmpty(i);
            Identifier id = ours.get(i);
            if (!id.toString().equals(entity.getStringOr("id", ""))) {
                findings.put(id.toString(), "came out of the fixer as " + entity);
                continue;
            }
            CompoundTag enchantments = entity.getListOrEmpty("Items").getCompoundOrEmpty(0)
                    .getCompoundOrEmpty("components").getCompoundOrEmpty("minecraft:enchantments");
            if (holdsItems.get(id) && (enchantments.contains("levels") || !enchantments.contains("minecraft:sharpness"))) {
                findings.put(id.toString(), "the sword it carries kept its 1.21.1 enchantments: " + enchantments);
            }
        }
        Sweep.report(context, "Every entity of ours, and what it carries, goes through the data fixer",
                findings, Map.of());
        context.succeed();
    }

    /**
     * A block chunk as 1.21.1 saved it: one block entity of every type of ours, each enchanted in its
     * own components (a placed bong is the device item itself, kept that way), every container also
     * holding an enchanted bow, every one that can be named carrying an anvil name, and a vanilla chest
     * holding the bow and the name as the control. Until 2.0.3 the fixer did not know the machines, so
     * their bows kept the {@code levels} wrapper 1.21.11 cannot read, and lost their enchantments on
     * load. Until the machines' {@code CustomName} was typed as text, a name kept 1.21.1's JSON string,
     * which 1.21.11 reads back as the literal text, quotes and all.
     */
    public static void theMachinesContentsAreUpgraded(GameTestHelper context) {
        Map<Identifier, Boolean> types = new TreeMap<>();
        Set<Identifier> nameable = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!Sweep.ours(BuiltInRegistries.BLOCK.getKey(block)) || !(block instanceof EntityBlock provider)) continue;
            BlockEntity entity = provider.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            if (entity == null) continue;
            Identifier type = BlockEntityType.getKey(entity.getType());
            if (type == null || !Sweep.ours(type)) continue;
            types.put(type, entity instanceof Container);
            if (entity instanceof Nameable) nameable.add(type);
        }
        Sweep.atLeast(context, "block entity types of ours", types.size(), 4);
        Sweep.atLeast(context, "block entity types of ours that keep a name", nameable.size(), 3);

        List<Identifier> order = new ArrayList<>(types.keySet());
        StringBuilder chunk = new StringBuilder("{DataVersion:3955,xPos:0,zPos:0,yPos:-4,Status:\"minecraft:full\","
                + "sections:[],block_entities:[" + machine("minecraft:chest", -1, true, true));
        for (int i = 0; i < order.size(); i++) {
            Identifier type = order.get(i);
            chunk.append(",").append(machine(type.toString(), i, types.get(type), nameable.contains(type)));
        }
        ListTag blockEntities = DataFixTypes.CHUNK.updateToCurrentVersion(context.getLevel().getServer().getFixerUpper(),
                read(chunk.append("]}").toString()), MINECRAFT_1_21_1).getListOrEmpty("block_entities");
        context.assertValueEqual(order.size() + 1, blockEntities.size(), "the block entities through the fixer");

        // The chest first: if it was not converted, the chunk itself did not go through; if its name
        // was not, the name below is not the shape 1.21.1 writes, and the machines' names prove nothing.
        CompoundTag chest = blockEntities.getCompoundOrEmpty(0);
        context.assertTrue(upgraded(chest.getListOrEmpty("Items").getCompoundOrEmpty(0)),
                "the control chest's bow kept its 1.21.1 enchantments, so the chunk never went through the fixer");
        context.assertTrue(NAME.equals(name(context, chest)),
                "the control chest's name did not come through the fixer: " + chest.get("CustomName"));
        Map<String, String> findings = new TreeMap<>();
        for (int i = 0; i < order.size(); i++) {
            Identifier type = order.get(i);
            CompoundTag blockEntity = blockEntities.getCompoundOrEmpty(i + 1);
            if (!upgraded(blockEntity)) {
                findings.put(type.toString(), "its own components kept their 1.21.1 enchantments: "
                        + blockEntity.getCompoundOrEmpty("components"));
            } else if (types.get(type) && !upgraded(blockEntity.getListOrEmpty("Items").getCompoundOrEmpty(0))) {
                findings.put(type.toString(), "the bow inside kept its 1.21.1 enchantments: "
                        + blockEntity.getListOrEmpty("Items"));
            } else if (nameable.contains(type) && !NAME.equals(name(context, blockEntity))) {
                findings.put(type.toString(), "its anvil name reads as \"" + name(context, blockEntity)
                        + "\" after the fixer: " + blockEntity.get("CustomName"));
            }
        }
        Sweep.report(context, "Every block entity of ours, and what it holds, goes through the data fixer",
                findings, Map.of());
        context.succeed();
    }

    /** 1.20.1's data version: what a world from the 1.20.1 build was saved as. */
    private static final int MINECRAFT_1_20_1 = 3465;

    /**
     * The values of ours as the 1.20.1 build keeps them in a stack's tag, one per component: the
     * component's id as the key and the component's own shape. Every component of ours must have one
     * here, so a new one is held to the lift the day it registers.
     */
    private static final Map<String, String> OLD_VALUES = Map.of(
            "hempdustry:smoke_contents", "{entries:[{strain:\"hempdustry:indica\",count:2}]}",
            "hempdustry:charges", "3",
            "hempdustry:potency", "2",
            "hempdustry:cooldown_until", "123456789L",
            "hempdustry:strength", "12",
            "hempdustry:quality", "\"clean\"");

    /**
     * A 1.20.1 stack carrying every value of ours, a vanilla value (its damage) and a key that is no
     * component at all, run through the real fixer. Minecraft 1.20.1 has no components, and vanilla's
     * 1.20.5 item fix leaves every tag key it does not know in {@code minecraft:custom_data}, where the
     * mod never looks ({@code ItemStackComponentizationFixMixin}). Each of ours must come back as its
     * own component, equal to what its codec reads from the old value; the damage is the control that
     * the stack went through the fix, and the stray key the control that the rest still lands in custom
     * data. A component of ours that changed shape since 1.21.1 fails here too.
     */
    public static void everyValueOfOursComesBackFrom1201(GameTestHelper context) {
        List<DataComponentType<?>> ours = new ArrayList<>();
        Map<String, String> findings = new TreeMap<>();
        for (DataComponentType<?> type : BuiltInRegistries.DATA_COMPONENT_TYPE) {
            Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (!Sweep.ours(id)) continue;
            ours.add(type);
            if (!OLD_VALUES.containsKey(id.toString())) {
                findings.put(id.toString(), "has no 1.20.1 value in this test; add the shape the 1.20.1 build writes");
            }
        }
        Sweep.atLeast(context, "data components of ours", ours.size(), 6);

        StringBuilder tag = new StringBuilder("{Damage:2,\"hempdustry:not_a_component\":7");
        OLD_VALUES.forEach((id, value) -> tag.append(",\"").append(id).append("\":").append(value));
        CompoundTag old = read("{id:\"hempdustry:wooden_pipe\",Count:1b,tag:" + tag.append("}}"));
        CompoundTag fixed = (CompoundTag) context.getLevel().getServer().getFixerUpper().update(
                References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, old), MINECRAFT_1_20_1,
                SharedConstants.getCurrentVersion().dataVersion().version()).getValue();
        DynamicOps<Tag> ops = context.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(ops, fixed).getOrThrow();

        context.assertTrue(stack.getDamageValue() == 2,
                "the stack's vanilla damage did not come through, so it never went through the fixer: " + fixed);
        CompoundTag leftover = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        context.assertTrue(leftover.contains("hempdustry:not_a_component"),
                "the stray key did not land in custom data, so the rest of the tag went missing: " + fixed);
        for (DataComponentType<?> type : ours) {
            String id = Objects.requireNonNull(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)).toString();
            String value = OLD_VALUES.get(id);
            if (value == null) continue;
            if (leftover.contains(id)) {
                findings.put(id, "was left in custom data");
                continue;
            }
            Object expected = type.codecOrThrow().parse(ops, readValue(value)).getOrThrow();
            if (!expected.equals(stack.get(type))) {
                findings.put(id, "came back as " + stack.get(type) + ", not " + expected);
            }
        }
        Sweep.report(context, "Every value of ours on a 1.20.1 stack comes back as its component", findings, Map.of());
        context.succeed();
    }

    /**
     * A block chunk as 1.20.1 saved it: one block entity of every type of ours, every container
     * holding an enchanted bow and a stack of five in 1.20.1's shape ({@code Count}, {@code tag}), a
     * placed bong holding its enchanted device as a whole stack under {@code Item} (the 1.20.1 build
     * has no components to keep it in), and a vanilla chest with the same as the control. A placed bong
     * is checked the way the game meets it, loaded and put in a level, by the components it hands its
     * drop: with the bong untyped in the fixer's table, or nothing taking up {@code Item}, it came up
     * a plain bong, its name, damage, enchantment and load gone.
     */
    public static void theMachinesContentsComeBackFrom1201(GameTestHelper context) {
        Map<Identifier, Block> types = new TreeMap<>();
        Map<Identifier, Boolean> containers = new HashMap<>();
        Map<Identifier, Boolean> bongs = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!Sweep.ours(BuiltInRegistries.BLOCK.getKey(block)) || !(block instanceof EntityBlock provider)) continue;
            BlockEntity entity = provider.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            if (entity == null) continue;
            Identifier type = BlockEntityType.getKey(entity.getType());
            if (type == null || !Sweep.ours(type) || types.containsKey(type)) continue;
            types.put(type, block);
            containers.put(type, entity instanceof Container);
            bongs.put(type, entity instanceof BongBlockEntity);
        }
        Sweep.atLeast(context, "block entity types of ours", types.size(), 4);
        context.assertTrue(bongs.containsValue(true), "no block entity of ours is a placed bong");

        List<Identifier> order = new ArrayList<>(types.keySet());
        StringBuilder chunk = new StringBuilder("{DataVersion:" + MINECRAFT_1_20_1 + ",xPos:0,zPos:0,yPos:-4,"
                + "Status:\"minecraft:full\",sections:[],block_entities:[" + oldBlockEntity("minecraft:chest", -1, true, null));
        for (int i = 0; i < order.size(); i++) {
            Identifier type = order.get(i);
            String device = bongs.get(type) ? BuiltInRegistries.ITEM.getKey(types.get(type).asItem()).toString() : null;
            chunk.append(",").append(oldBlockEntity(type.toString(), i, containers.get(type), device));
        }
        ListTag blockEntities = DataFixTypes.CHUNK.updateToCurrentVersion(context.getLevel().getServer().getFixerUpper(),
                read(chunk.append("]}").toString()), MINECRAFT_1_20_1).getListOrEmpty("block_entities");
        context.assertValueEqual(order.size() + 1, blockEntities.size(), "the block entities through the fixer");

        context.assertTrue(oldContentsUpgraded(blockEntities.getCompoundOrEmpty(0)),
                "the control chest's stacks kept their 1.20.1 shape, so the chunk never went through the fixer: "
                        + blockEntities.getCompoundOrEmpty(0));
        Map<String, String> findings = new TreeMap<>();
        for (int i = 0; i < order.size(); i++) {
            Identifier type = order.get(i);
            CompoundTag fixed = blockEntities.getCompoundOrEmpty(i + 1);
            if (containers.get(type) && !oldContentsUpgraded(fixed)) {
                findings.put(type.toString(), "its stacks kept their 1.20.1 shape: " + fixed.get("Items"));
            } else if (bongs.get(type)) {
                BlockEntity loaded = BlockEntity.loadStatic(context.absolutePos(new BlockPos(1, 1, 1)),
                        types.get(type).defaultBlockState(), fixed, context.getLevel().registryAccess());
                if (loaded == null) {
                    findings.put(type.toString(), "did not load after the fixer: " + fixed);
                    continue;
                }
                loaded.setLevel(context.getLevel());
                if (!loaded.collectComponents().has(DataComponents.ENCHANTMENTS)) {
                    findings.put(type.toString(), "the placed device lost its enchantment: " + fixed);
                }
            }
        }
        Sweep.report(context, "Every block entity of ours, and what it holds, comes back from 1.20.1",
                findings, Map.of());
        context.succeed();
    }

    /** A 1.20.1 stack whose tag only the item fixes understand: they turn its enchantments into a component. */
    private static String oldEnchanted(String id, String enchantment, int count, int slot) {
        return "{Slot:" + slot + "b,id:\"" + id + "\",Count:" + count + "b,"
                + "tag:{Enchantments:[{id:\"minecraft:" + enchantment + "\",lvl:3s}]}}";
    }

    private static String oldBlockEntity(String id, int x, boolean holdsItems, String device) {
        return "{id:\"" + id + "\",x:" + x + ",y:-60,z:0"
                + (holdsItems ? ",Items:[" + oldEnchanted("minecraft:bow", "power", 1, 0)
                        + ",{Slot:1b,id:\"minecraft:stick\",Count:5b}]" : "")
                + (device != null ? ",Item:" + oldEnchanted(device, "unbreaking", 1, 0) : "") + "}";
    }

    /** Whether a container's bow and its stack of five both came out of 1.20.1 in this version's shape. */
    private static boolean oldContentsUpgraded(CompoundTag blockEntity) {
        ListTag items = blockEntity.getListOrEmpty("Items");
        return upgraded(items.getCompoundOrEmpty(0)) && items.getCompoundOrEmpty(1).getIntOr("count", 0) == 5;
    }

    private static Tag readValue(String snbt) {
        return read("{v:" + snbt + "}").get("v");
    }

    /** The anvil name every nameable block entity carries in the 1.21.1 chunk. */
    private static final String NAME = "Hemp Kitchen";

    private static String machine(String id, int x, boolean holdsItems, boolean named) {
        String bow = "{Slot:0b,id:\"minecraft:bow\",count:1,"
                + "components:{\"minecraft:enchantments\":{levels:{\"minecraft:power\":3}}}}";
        // 1.21.1 saves a name as its JSON, and an anvil's plain name collapses to a JSON string.
        return "{id:\"" + id + "\",x:" + x + ",y:-60,z:0,"
                + "components:{\"minecraft:enchantments\":{levels:{\"minecraft:unbreaking\":3}}}"
                + (holdsItems ? ",Items:[" + bow + "]" : "")
                + (named ? ",CustomName:'\"" + NAME + "\"'" : "") + "}";
    }

    /** Whether a stack's or a block entity's enchantments lost the 1.21.1 {@code levels} wrapper. */
    private static boolean upgraded(CompoundTag holder) {
        CompoundTag enchantments = holder.getCompoundOrEmpty("components").getCompoundOrEmpty("minecraft:enchantments");
        return !enchantments.isEmpty() && !enchantments.contains("levels");
    }

    /** A block entity's name as this version reads it back, or null when it has none it can read. */
    private static String name(GameTestHelper context, CompoundTag blockEntity) {
        Tag name = blockEntity.get("CustomName");
        if (name == null) return null;
        return ComponentSerialization.CODEC
                .parse(context.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), name)
                .result().map(Component::getString).orElse(null);
    }

    private static CompoundTag read(String snbt) {
        try {
            return TagParser.parseCompoundFully(snbt);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
