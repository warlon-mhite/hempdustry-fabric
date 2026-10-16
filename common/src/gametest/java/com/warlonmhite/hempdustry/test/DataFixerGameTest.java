package com.warlonmhite.hempdustry.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
