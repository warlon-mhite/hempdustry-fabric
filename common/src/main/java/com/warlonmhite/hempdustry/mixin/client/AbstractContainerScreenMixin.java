package com.warlonmhite.hempdustry.mixin.client;

import com.warlonmhite.hempdustry.client.StashJarMouseActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every container screen offers the Stash Jar's scroll menu beside the bundle's, as vanilla adds its
 * own in {@code init}.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    protected abstract void addItemSlotMouseAction(ItemSlotMouseAction action);

    @Inject(method = "init", at = @At("TAIL"))
    private void hempdustry$addStashJarMenu(CallbackInfo ci) {
        addItemSlotMouseAction(new StashJarMouseActions(Minecraft.getInstance()));
    }
}
