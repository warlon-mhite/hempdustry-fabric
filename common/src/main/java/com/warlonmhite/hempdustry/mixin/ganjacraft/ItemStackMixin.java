package com.warlonmhite.hempdustry.mixin.ganjacraft;

import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A GanjaCraft item stack, wherever it was saved, reads as the Hempdustry item it became. */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @ModifyVariable(method = "of", at = @At("HEAD"), argsOnly = true)
    private static CompoundTag hempdustry$readGanjaCraft(CompoundTag saved) {
        CompoundTag converted = GanjaCraftWorlds.item(saved);
        return converted != null ? converted : saved;
    }
}
