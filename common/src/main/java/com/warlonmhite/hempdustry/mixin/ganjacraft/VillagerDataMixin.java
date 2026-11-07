package com.warlonmhite.hempdustry.mixin.ganjacraft;

import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A villager's profession, converted before it is read: a profession the game does not know makes it
 * drop the villager's whole data -- level, experience and type with it -- so the old mod's is replaced
 * first (Hemp-Craft's Hemp Farmer becomes a farmer, keeping the trades it had rolled). A zombie
 * villager keeps the same data, and gets it back when cured.
 */
@Mixin({Villager.class, ZombieVillager.class})
public class VillagerDataMixin {

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void hempdustry$readGanjaCraft(CompoundTag saved, CallbackInfo ci) {
        if (!saved.contains("VillagerData", Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag data = saved.getCompound("VillagerData");
        String becomes = GanjaCraftWorlds.profession(data.getString("profession"));
        if (becomes != null) {
            data.putString("profession", becomes);
        }
    }
}
