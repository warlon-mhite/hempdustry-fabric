package com.warlonmhite.hempdustry.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.client.render.FlipFlopsModel;
import com.warlonmhite.hempdustry.client.render.HempBeanieRenderer;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The flip-flops and the tam, drawn where the armour layer would draw boots and a helmet. Fabric's
 * armour hook lets an item replace its own armour piece; Forge 47 asks the item's own class, and these
 * two are the outfit's plain armour items, which share a material with the shirt and the trousers. So
 * the armour layer draws nothing for them ({@code HumanoidArmorLayerMixin}) and this layer, beside it
 * on every body that wears armour, draws them as Fabric's hook does.
 */
final class OutfitLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private HumanoidModel<LivingEntity> flipFlops;

    OutfitLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack feet = entity.getItemBySlot(EquipmentSlot.FEET);
        if (feet.is(ModItems.FLIP_FLOPS)) {
            if (flipFlops == null) {
                // The layer is baked with the entity models, after this layer is made.
                flipFlops = FlipFlopsModel.create(Minecraft.getInstance().getEntityModels());
            }
            FlipFlopsModel.pose((HumanoidModel<LivingEntity>) getParentModel(), flipFlops);
            flipFlops.renderToBuffer(poseStack, ItemRenderer.getArmorFoilBuffer(buffers,
                    RenderType.armorCutoutNoCull(FlipFlopsModel.TEXTURE), false, feet.hasFoil()), light, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(ModItems.HEMP_BEANIE)) {
            HempBeanieRenderer.render(poseStack, buffers, head, entity, light, getParentModel());
        }
    }
}
