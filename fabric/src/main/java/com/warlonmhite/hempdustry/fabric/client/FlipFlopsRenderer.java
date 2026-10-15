package com.warlonmhite.hempdustry.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.client.render.FlipFlopsModel;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** {@link FlipFlopsModel} through Fabric's armour hook, which replaces the boots layer for this item. */
public final class FlipFlopsRenderer implements ArmorRenderer {
    private HumanoidModel<LivingEntity> model;

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack,
                       LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel) {
        if (model == null) {
            // The layer is baked with the entity models, after this renderer is registered.
            model = FlipFlopsModel.create(Minecraft.getInstance().getEntityModels());
        }
        FlipFlopsModel.pose(contextModel, model);
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model, FlipFlopsModel.TEXTURE);
    }
}
