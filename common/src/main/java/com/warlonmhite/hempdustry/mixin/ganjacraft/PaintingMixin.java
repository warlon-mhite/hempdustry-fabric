package com.warlonmhite.hempdustry.mixin.ganjacraft;

import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.decoration.Painting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A painting hung with a GanjaCraft motive shows the Hempdustry painting of the same subject. */
@Mixin(Painting.class)
public class PaintingMixin {

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void hempdustry$readGanjaCraft(CompoundTag saved, CallbackInfo ci) {
        String becomes = GanjaCraftWorlds.painting(saved.getString("Motive"));
        if (becomes != null) {
            saved.putString("Motive", becomes);
        }
    }
}
