package com.warlonmhite.hempdustry.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Tells vanilla's data fixer what the two hemp boats, the three machines and a placed bong are, so a world moved to a
 * newer Minecraft is upgraded around them instead of stopping at them, and what they hold is upgraded
 * too.
 *
 * <p>When a world saved by an older version is opened, every entity chunk goes through the data
 * fixer, which reads each entity by its {@code id} against the schema's table of entity types. An
 * id it has never heard of does not fail on its own: <b>the whole chunk's {@code Entities} list
 * fails to read, and the chunk is passed through unfixed</b>. So a hemp boat left the vanilla
 * entities beside it in their old format. Measured on 1.21.1 to 1.21.11: an armour stand in the
 * same chunk kept {@code ArmorItems}, which 1.21.11 no longer reads, and came up with no armour at
 * all; the same stand beside a vanilla boat or a pig came through intact. The boats' own data survived
 * either way, because the mod reads its own format.
 *
 * <p>Block entities fail more quietly: an unknown one is passed through on its own and the chunk
 * around it is fixed. But nothing inside it is, so the items in a Decarboxylator or an Infuser kept
 * their 1.21.1 shape and 1.21.11 could not read their components — an enchanted bow in an oven's
 * fuel slot came out with no enchantments and a bare "Serialization errors" in the log, while the
 * same bow in a vanilla chest was converted.
 *
 * <p>The fix is to be in the table. Our two boats are registered the way vanilla registers its own:
 * the boat as a plain entity, the chest boat with its {@code Items} typed as item stacks, which is
 * what lets the item fixes reach what it carries (an enchanted book in a chest boat changed format
 * on the way to 1.21.11 too). Nothing else about either boat has ever needed fixing. The machines
 * are registered as vanilla registers its furnace: their {@code Items} typed as item stacks, which is
 * the shape {@code Inventories.writeData} writes.
 *
 * <h2>Why here</h2>
 *
 * Schema 1460 is the last one that builds its entity and block entity tables from scratch; every
 * later schema asks its parent for the table and edits it, and DFU's own
 * {@code Schema#registerEntities} and {@code #registerBlockEntities} are exactly that call. So this
 * runs once per later schema, each time for that schema, and our ids are in every table from 1460 to
 * the current one. Nothing is stored and no fix is added: a hemp boat's or a
 * machine's own data passes through untouched, and only the vanilla data around and inside them is
 * now fixed as it always should have been. Worlds that never change version never run any of this.
 *
 * <p>{@code remap = false} on the injectors because both methods are DFU's, not Minecraft's, and have
 * no mapping; the class itself is still remapped through the refmap.
 */
@Mixin(Schema1460.class)
public class Schema1460Mixin {
    @Inject(method = "registerEntities", at = @At("RETURN"), remap = false)
    private void hempdustry$registerHempBoats(Schema schema,
                                              CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> cir) {
        Map<String, Supplier<TypeTemplate>> entities = cir.getReturnValue();
        schema.registerSimple(entities, "hempdustry:hemp_boat");
        schema.register(entities, "hempdustry:hemp_chest_boat",
                name -> DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema))));
    }

    @Inject(method = "registerBlockEntities", at = @At("RETURN"), remap = false)
    private void hempdustry$registerMachines(Schema schema,
                                             CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> cir) {
        Map<String, Supplier<TypeTemplate>> blockEntities = cir.getReturnValue();
        for (String machine : new String[] {"hempdustry:decarboxylator", "hempdustry:infuser", "hempdustry:hemp_press"}) {
            schema.register(blockEntities, machine,
                    name -> DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema))));
        }
        // A placed bong is the device item itself, kept as the block entity's own components
        // (BongBlockEntity); being in the table is what lets the fixer reach them.
        schema.registerSimple(blockEntities, "hempdustry:bong");
    }
}
