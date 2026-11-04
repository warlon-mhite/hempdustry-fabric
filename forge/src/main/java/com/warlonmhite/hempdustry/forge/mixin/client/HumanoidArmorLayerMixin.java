package com.warlonmhite.hempdustry.forge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The flip-flops' and the tam's armour piece, as the armour layer would draw it: nothing, nor a trim
 * or a glint on it. The outfit layer draws them ({@code OutfitLayer}); Fabric's armour hook replaces
 * the piece the same way. On 1.20.1 Forge asks the item itself for anything else, and these two are
 * plain armour items.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, A extends HumanoidModel<T>> {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void hempdustry$skipOutfit(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot,
                                       int light, A model, CallbackInfo ci) {
        ItemStack worn = entity.getItemBySlot(slot);
        if (worn.is(ModItems.FLIP_FLOPS) || worn.is(ModItems.HEMP_BEANIE)) {
            ci.cancel();
        }
    }
}
