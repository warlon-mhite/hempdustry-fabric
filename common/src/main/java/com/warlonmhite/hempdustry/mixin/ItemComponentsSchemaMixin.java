package com.warlonmhite.hempdustry.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.V3818_3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells vanilla's data fixer that a carried Stash Jar's {@code hempdustry:stash_contents} holds item stacks, as it
 * knows a bundle's {@code minecraft:bundle_contents} and a shulker box's {@code minecraft:container} do, so the buds
 * in a jar in someone's inventory are upgraded with a world moved to a newer Minecraft.
 *
 * <p>The fixer reaches into a stack's components only through the list of component types its schema names; an id
 * it does not know passes through untouched, and so does everything inside it. A placed jar was already covered
 * ({@code Schema1460Mixin} types its block entity's {@code contents}); a jar broken and carried, or kept in a chest,
 * holds the same entries as an item component, which nothing typed. Measured with a 1.21.1-shaped stack: a bud named
 * in an anvil kept its name as 1.21.1's JSON string inside the jar, and reads back as the JSON, quotes and all, while
 * the same bud in a bundle was converted.
 *
 * <h2>Why here</h2>
 *
 * Every schema since components exist (3818.3) builds its component list by calling the one before it, back to
 * {@code V3818_3.components}: 4059, 4307 and 4996 each take that list and edit it. Adding the entry where the list
 * is first made puts it in every later list as well, a vanilla schema added after 26.3 included as long as it keeps
 * building on its parent's list. The entry is the shape {@code StashContents.CODEC} writes: a list of entries, each
 * naming its item as a stack under {@code item}.
 */
@Mixin(V3818_3.class)
public class ItemComponentsSchemaMixin {
    @Inject(method = "components", at = @At("RETURN"))
    private static void hempdustry$typeStashContents(Schema schema,
                                                     CallbackInfoReturnable<SequencedMap<String, Supplier<TypeTemplate>>> cir) {
        cir.getReturnValue().put("hempdustry:stash_contents",
                () -> DSL.list(DSL.optionalFields("item", References.ITEM_STACK.in(schema))));
    }
}
