package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.item.custom.StashJarItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The bundle menu's choice, sent in vanilla's packet, reaches a Stash Jar too: the server sets it on
 * {@code hempdustry:stash_contents}, where {@code BundleItem.toggleSelectedItem} only knows
 * {@code bundle_contents}.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Inject(method = "setSelectedBundleItemIndex", at = @At("HEAD"))
    private void hempdustry$selectInStashJar(int slotIndex, int selectedItemIndex, CallbackInfo ci) {
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            ItemStack stack = slots.get(slotIndex).getItem();
            if (stack.getItem() instanceof StashJarItem) {
                StashJarItem.toggleSelected(stack, selectedItemIndex);
            }
        }
    }
}
