package com.warlonmhite.hempdustry.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.test.TestContext;

/**
 * A hemp boat must not stop vanilla's data fixer from upgrading the entities beside it, and the items
 * inside the mod's machines must be upgraded like the items in a chest.
 *
 * <p>An entity chunk as 1.21.1 saved it, run through the real fixer to this version: a hemp boat, a
 * hemp chest boat carrying an enchanted sword, and a vanilla armour stand wearing boots. Before the
 * boats were registered with the fixer ({@code Schema1460Mixin}) it could not read the chunk's entity
 * list at all and passed it through untouched, so the stand never got the {@code equipment} map this
 * version reads its armour from — in a real world, it came up bare. The sword is the chest boat's
 * half: its enchantments changed shape on the way (1.21.5 dropped the {@code levels} wrapper), and
 * only a chest boat whose {@code Items} the fixer knows about gets them converted.
 */
public final class DataFixerGameTest {

    /** 1.21.1's data version: what a world coming across from the 1.21.1 line was saved as. */
    private static final int MINECRAFT_1_21_1 = 3955;

    private static final String CHUNK = "{DataVersion:3955,Position:[I;0,0],Entities:["
            + "{id:\"hempdustry:hemp_boat\",Pos:[0.5d,-60.0d,0.5d],Type:\"oak\"},"
            + "{id:\"hempdustry:hemp_chest_boat\",Pos:[3.5d,-60.0d,0.5d],Type:\"oak\",Items:["
            + "{Slot:0b,id:\"minecraft:iron_sword\",count:1,"
            + "components:{\"minecraft:enchantments\":{levels:{\"minecraft:sharpness\":3}}}}]},"
            + "{id:\"minecraft:armor_stand\",Pos:[6.5d,-60.0d,0.5d],"
            + "ArmorItems:[{id:\"minecraft:leather_boots\",count:1},{},{},{}],HandItems:[{},{}]}]}";

    public static void aHempBoatDoesNotStopTheDataFixer(TestContext context) {
        NbtCompound fixed = DataFixTypes.ENTITY_CHUNK.update(
                context.getWorld().getServer().getDataFixer(), read(CHUNK), MINECRAFT_1_21_1);
        NbtList entities = fixed.getListOrEmpty("Entities");

        // The fixer writes the new equipment map and leaves the old key beside it; the game drops the
        // old one the next time it saves the entity. The new one is what 1.21.11 reads.
        NbtCompound stand = entities.getCompoundOrEmpty(2);
        context.assertTrue("minecraft:leather_boots".equals(
                        stand.getCompoundOrEmpty("equipment").getCompoundOrEmpty("feet").getString("id", "")),
                "the armour stand beside the hemp boats was not upgraded, so it has no equipment: " + stand);

        NbtCompound enchantments = entities.getCompoundOrEmpty(1).getListOrEmpty("Items").getCompoundOrEmpty(0)
                .getCompoundOrEmpty("components").getCompoundOrEmpty("minecraft:enchantments");
        context.assertTrue(!enchantments.contains("levels") && enchantments.contains("minecraft:sharpness"),
                "the sword in the hemp chest boat kept its 1.21.1 enchantments: " + enchantments);

        context.assertTrue("hempdustry:hemp_boat".equals(entities.getCompoundOrEmpty(0).getString("id", "")),
                "the hemp boat itself did not come through: " + entities.getCompoundOrEmpty(0));
        context.complete();
    }

    /**
     * A block chunk as 1.21.1 saved it: a Decarboxylator, an Infuser and a Hemp Press each holding an
     * enchanted bow, a placed bong that is itself enchanted, and a vanilla chest holding the bow as the
     * control. Until 2.0.3 the fixer did not know
     * the machines, so their bows kept the {@code levels} wrapper 1.21.11 cannot read, and lost their
     * enchantments on load.
     */
    private static final String BLOCK_CHUNK = "{DataVersion:3955,xPos:0,zPos:0,yPos:-4,"
            + "Status:\"minecraft:full\",sections:[],block_entities:["
            + machine("hempdustry:decarboxylator", 0, 4) + "," + machine("hempdustry:infuser", 1, 1) + ","
            + machine("hempdustry:hemp_press", 3, 0) + ","
            + "{id:\"hempdustry:bong\",x:4,y:-60,z:0,"
            + "components:{\"minecraft:enchantments\":{levels:{\"minecraft:unbreaking\":3}}}},"
            + machine("minecraft:chest", 2, 0) + "]}";

    private static String machine(String id, int x, int slot) {
        return "{id:\"" + id + "\",x:" + x + ",y:-60,z:0,Items:[{Slot:" + slot + "b,id:\"minecraft:bow\","
                + "count:1,components:{\"minecraft:enchantments\":{levels:{\"minecraft:power\":3}}}}]}";
    }

    public static void theMachinesContentsAreUpgraded(TestContext context) {
        NbtCompound fixed = DataFixTypes.CHUNK.update(
                context.getWorld().getServer().getDataFixer(), read(BLOCK_CHUNK), MINECRAFT_1_21_1);
        NbtList blockEntities = fixed.getListOrEmpty("block_entities");
        context.assertEquals(5, blockEntities.size(), "the block entities through the fixer: " + fixed);

        // The chest first: if it was not converted, the chunk itself did not go through.
        for (int i : new int[] {4, 0, 1, 2}) {
            NbtCompound blockEntity = blockEntities.getCompoundOrEmpty(i);
            NbtCompound enchantments = blockEntity.getListOrEmpty("Items").getCompoundOrEmpty(0)
                    .getCompoundOrEmpty("components").getCompoundOrEmpty("minecraft:enchantments");
            context.assertTrue(!enchantments.contains("levels") && enchantments.contains("minecraft:power"),
                    "the bow in " + blockEntity.getString("id", "?") + " kept its 1.21.1 enchantments: "
                            + enchantments);
        }
        // A placed bong keeps the whole device as its own components, not in an Items list.
        NbtCompound bong = blockEntities.getCompoundOrEmpty(3).getCompoundOrEmpty("components")
                .getCompoundOrEmpty("minecraft:enchantments");
        context.assertTrue(!bong.contains("levels") && bong.contains("minecraft:unbreaking"),
                "the placed bong kept its 1.21.1 enchantments: " + bong);
        context.complete();
    }

    private static NbtCompound read(String snbt) {
        try {
            return StringNbtReader.readCompound(snbt);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
