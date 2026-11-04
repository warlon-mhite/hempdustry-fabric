package com.warlonmhite.hempdustry.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.warlonmhite.hempdustry.block.entity.custom.BongBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * A world made on Minecraft 1.20.1 must open here with everything of ours in it: the mod has a build
 * for 1.20.1, and vanilla's data fixer carries such a world up to this version only as far as it
 * knows what it is reading. Each test runs data in 1.20.1's own shape through the real fixer.
 *
 * <p>Measured with a world built on the 1.20.1 build and opened here, before any of this: every value
 * of ours on a stack sat in {@code minecraft:custom_data} (a packed pipe came up unpacked), the
 * stacks in the machines and the hemp chest boat came up as one item each with nothing on them, an
 * armour stand beside a hemp boat lost its shirt's trim, and a placed bong lost its name, damage,
 * enchantment and load.
 *
 * <p>Everything is built from the registries rather than from a list, so a new entity, block entity
 * or component is checked the day it registers.
 */
public final class DataFixerGameTest {

    /** 1.20.1's data version: what a world from the 1.20.1 build was saved as. */
    private static final int MINECRAFT_1_20_1 = 3465;

    /** A 1.20.1 stack whose tag only the item fixes understand: they turn its enchantments into a component. */
    private static String enchanted(String id, String enchantment, int count, int slot) {
        return "{Slot:" + slot + "b,id:\"" + id + "\",Count:" + count + "b,"
                + "tag:{Enchantments:[{id:\"minecraft:" + enchantment + "\",lvl:3s}]}}";
    }

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
     * component at all. After the fixer, each of ours must be its own component again and equal to
     * what its codec reads from the old value; the vanilla one is the control that the stack went
     * through the fix, and the stray key the control that the rest still lands in custom data.
     */
    @GameTest(template = HempdustryGameTests.EMPTY)
    public static void everyValueOfOursComesBackFrom1201(GameTestHelper context) {
        List<DataComponentType<?>> ours = new ArrayList<>();
        Map<String, String> findings = new TreeMap<>();
        for (DataComponentType<?> type : BuiltInRegistries.DATA_COMPONENT_TYPE) {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
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
                References.ITEM_STACK,
                new Dynamic<>(NbtOps.INSTANCE, old), MINECRAFT_1_20_1,
                SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        ItemStack stack = ItemStack.parseOptional(context.getLevel().registryAccess(), fixed);

        context.assertTrue(stack.getDamageValue() == 2,
                "the stack's vanilla damage did not come through, so it never went through the fixer: " + fixed);
        CompoundTag leftover = stack.getOrDefault(DataComponents.CUSTOM_DATA,
                CustomData.EMPTY).copyTag();
        context.assertTrue(leftover.contains("hempdustry:not_a_component"),
                "the stray key did not land in custom data, so the rest of the tag went missing: " + fixed);
        DynamicOps<Tag> ops = context.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
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
     * An entity chunk as 1.20.1 saved it: every entity type of ours, each carrying an enchanted
     * sword, beside a vanilla armour stand in damaged boots. Without our entities in the fixer's table
     * ({@code Schema1460Mixin}) the whole chunk's entity list is passed through unread, and the stand's
     * boots, and the swords, keep a shape this version reads as a bare item.
     */
    @GameTest(template = HempdustryGameTests.EMPTY)
    public static void aHempBoatDoesNotStopTheDataFixer(GameTestHelper context) {
        List<ResourceLocation> ours = new ArrayList<>();
        Map<ResourceLocation, Boolean> holdsItems = new HashMap<>();
        StringBuilder chunk = new StringBuilder("{DataVersion:" + MINECRAFT_1_20_1 + ",Position:[I;0,0],Entities:[");
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!Sweep.ours(id)) continue;
            Entity entity = type.create(context.getLevel());
            holdsItems.put(id, entity instanceof Container);
            chunk.append("{id:\"").append(id).append("\",Pos:[").append(ours.size() * 3).append(".5d,-60.0d,0.5d],")
                    .append("Type:\"oak\",Items:[").append(enchanted("minecraft:iron_sword", "sharpness", 1, 0))
                    .append("]},");
            ours.add(id);
        }
        chunk.append("{id:\"minecraft:armor_stand\",Pos:[-3.5d,-60.0d,0.5d],"
                + "ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{Damage:5}},{},{},{}],HandItems:[{},{}]}]}");
        Sweep.atLeast(context, "entity types of ours", ours.size(), 2);

        ListTag entities = DataFixTypes.ENTITY_CHUNK.updateToCurrentVersion(
                context.getLevel().getServer().getFixerUpper(), read(chunk.toString()), MINECRAFT_1_20_1)
                .getList("Entities", Tag.TAG_COMPOUND);

        CompoundTag boots = entities.getCompound(ours.size()).getList("ArmorItems", Tag.TAG_COMPOUND).getCompound(0);
        context.assertTrue(boots.getCompound("components").getInt("minecraft:damage") == 5,
                "the armour stand beside " + ours + " was not upgraded, so its boots lost their damage: " + boots);

        Map<String, String> findings = new TreeMap<>();
        for (int i = 0; i < ours.size(); i++) {
            CompoundTag entity = entities.getCompound(i);
            ResourceLocation id = ours.get(i);
            if (!id.toString().equals(entity.getString("id"))) {
                findings.put(id.toString(), "came out of the fixer as " + entity);
            } else if (holdsItems.get(id) && !upgraded(entity.getList("Items", Tag.TAG_COMPOUND).getCompound(0))) {
                findings.put(id.toString(), "the sword it carries kept its 1.20.1 shape: " + entity.get("Items"));
            }
        }
        Sweep.report(context, "Every entity of ours, and what it carries, goes through the data fixer",
                findings, Map.of());
        context.succeed();
    }

    /**
     * A block chunk as 1.20.1 saved it: one block entity of every type of ours, every container
     * holding an enchanted bow and a stack of five, a placed bong holding its enchanted device, and a
     * vanilla chest with the same as the control. Until the machines were in the fixer's table their
     * stacks kept 1.20.1's {@code Count}, which this version does not read: five came up as one. A
     * placed bong is checked the way the game meets it, loaded and put in a level, by the components
     * it hands its drop.
     */
    @GameTest(template = HempdustryGameTests.EMPTY)
    public static void theMachinesContentsAreUpgraded(GameTestHelper context) {
        Map<ResourceLocation, Block> types = new TreeMap<>();
        Map<ResourceLocation, Boolean> containers = new HashMap<>();
        Map<ResourceLocation, Boolean> bongs = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!Sweep.ours(BuiltInRegistries.BLOCK.getKey(block)) || !(block instanceof EntityBlock provider)) continue;
            BlockEntity entity = provider.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            if (entity == null) continue;
            ResourceLocation type = BlockEntityType.getKey(entity.getType());
            if (type == null || !Sweep.ours(type) || types.containsKey(type)) continue;
            types.put(type, block);
            containers.put(type, entity instanceof Container);
            bongs.put(type, entity instanceof BongBlockEntity);
        }
        Sweep.atLeast(context, "block entity types of ours", types.size(), 4);

        List<ResourceLocation> order = new ArrayList<>(types.keySet());
        StringBuilder chunk = new StringBuilder("{DataVersion:" + MINECRAFT_1_20_1 + ",xPos:0,zPos:0,yPos:-4,"
                + "Status:\"minecraft:full\",sections:[],block_entities:[" + blockEntity("minecraft:chest", -1, true, null));
        for (int i = 0; i < order.size(); i++) {
            ResourceLocation type = order.get(i);
            String device = bongs.get(type) ? BuiltInRegistries.ITEM.getKey(types.get(type).asItem()).toString() : null;
            chunk.append(",").append(blockEntity(type.toString(), i, containers.get(type), device));
        }
        ListTag blockEntities = DataFixTypes.CHUNK.updateToCurrentVersion(context.getLevel().getServer().getFixerUpper(),
                read(chunk.append("]}").toString()), MINECRAFT_1_20_1).getList("block_entities", Tag.TAG_COMPOUND);
        context.assertValueEqual(blockEntities.size(), order.size() + 1, "the block entities through the fixer");

        context.assertTrue(contentsUpgraded(blockEntities.getCompound(0)),
                "the control chest's stacks kept their 1.20.1 shape, so the chunk never went through the fixer: "
                        + blockEntities.getCompound(0));
        Map<String, String> findings = new TreeMap<>();
        for (int i = 0; i < order.size(); i++) {
            ResourceLocation type = order.get(i);
            CompoundTag fixed = blockEntities.getCompound(i + 1);
            if (containers.get(type) && !contentsUpgraded(fixed)) {
                findings.put(type.toString(), "its stacks kept their 1.20.1 shape: " + fixed.get("Items"));
            } else if (bongs.get(type)) {
                BlockPos pos = context.absolutePos(new BlockPos(1, 1, 1));
                BlockEntity loaded = BlockEntity.loadStatic(pos, types.get(type).defaultBlockState(), fixed,
                        context.getLevel().registryAccess());
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
        Sweep.report(context, "Every block entity of ours, and what it holds, goes through the data fixer",
                findings, Map.of());
        context.succeed();
    }

    private static String blockEntity(String id, int x, boolean holdsItems, String device) {
        return "{id:\"" + id + "\",x:" + x + ",y:-60,z:0"
                + (holdsItems ? ",Items:[" + enchanted("minecraft:bow", "power", 1, 0)
                        + ",{Slot:1b,id:\"minecraft:stick\",Count:5b}]" : "")
                + (device != null ? ",Item:" + enchanted(device, "unbreaking", 1, 0) : "") + "}";
    }

    /** Whether a 1.20.1 stack came out in this version's shape: its enchantments a component. */
    private static boolean upgraded(CompoundTag stack) {
        return stack.getCompound("components").contains("minecraft:enchantments");
    }

    /** Whether a container's bow and its stack of five both came out in this version's shape. */
    private static boolean contentsUpgraded(CompoundTag blockEntity) {
        ListTag items = blockEntity.getList("Items", Tag.TAG_COMPOUND);
        return upgraded(items.getCompound(0)) && items.getCompound(1).getInt("count") == 5;
    }

    private static CompoundTag read(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Tag readValue(String snbt) {
        return read("{v:" + snbt + "}").get("v");
    }
}
