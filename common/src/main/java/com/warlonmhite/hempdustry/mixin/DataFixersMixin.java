package com.warlonmhite.hempdustry.mixin;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.datafix.DataFixers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Drops a placed Hemp Bed's block entity from a world saved before 26.2, as vanilla drops its own
 * beds'.
 *
 * <p>Up to 1.21.11 every bed had a block entity, which drew it; 26.2 draws a bed from its block
 * model and has none. Vanilla's fixer removes the old {@code minecraft:bed} entries at schema 4885
 * ({@code RemoveBlockEntityTagFix}, the only one built from a schema and a set). The Hemp Bed's
 * {@code hempdustry:hemp_bed} entries go in the same set: otherwise each one in an old world is an
 * unknown block entity type, logged as an error every time its chunk loads until something rewrites
 * the chunk. What the entry held — whatever its item carried, an anvil name among them — goes with
 * it, as it does for vanilla's beds.
 */
@Mixin(DataFixers.class)
public class DataFixersMixin {
    @ModifyArg(
            method = "addFixers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/datafix/fixes/RemoveBlockEntityTagFix;<init>(Lcom/mojang/datafixers/schemas/Schema;Ljava/util/Set;)V"
            ),
            index = 1,
            remap = false
    )
    private static Set<String> hempdustry$removeHempBeds(Set<String> ids) {
        if (!ids.contains("minecraft:bed")) {
            return ids;
        }
        Set<String> withHemp = new HashSet<>(ids);
        withHemp.add("hempdustry:hemp_bed");
        return Set.copyOf(withHemp);
    }
}
