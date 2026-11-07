package com.warlonmhite.hempdustry.mixin.ganjacraft;

import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A GanjaCraft entity reads as the one it became (its weed man, a villager), or not at all. */
@Mixin(EntityType.class)
public class EntityTypeMixin {

    @Inject(method = "by", at = @At("HEAD"), cancellable = true)
    private static void hempdustry$readGanjaCraft(CompoundTag saved, CallbackInfoReturnable<Optional<EntityType<?>>> cir) {
        Optional<String> becomes = GanjaCraftWorlds.entity(saved.getString("id"));
        if (becomes != null) {
            cir.setReturnValue(becomes.flatMap(EntityType::byString));
        }
    }
}
