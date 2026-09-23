package com.warlonmhite.hempdustry.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.test.TestContext;

/**
 * A hemp boat must not stop vanilla's data fixer from upgrading the entities beside it.
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

    private static NbtCompound read(String snbt) {
        try {
            return StringNbtReader.readCompound(snbt);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
