package com.warlonmhite.hempdustry.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The flip-flops' and the tam's armour piece, as the armour layer draws it: nothing, and no layer of
 * it -- {@link OutfitLayer} draws them, so the boots and the helmet the outfit's material would give
 * them never show, nor a trim or a glint on them.
 */
final class OutfitExtensions implements IClientItemExtensions {
    private static final Model NOTHING = new Model(RenderType::armorCutoutNoCull) {
        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        }
    };

    @Override
    public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        return NOTHING;
    }

    @Override
    public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, net.minecraft.world.item.ArmorMaterial.Layer layer,
                                      int layerIdx, int fallbackColor) {
        // 0 skips a layer.
        return 0;
    }
}
