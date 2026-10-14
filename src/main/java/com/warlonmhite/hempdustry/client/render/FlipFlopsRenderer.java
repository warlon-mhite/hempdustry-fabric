package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Flip-flops drawn as flip-flops: a sole under each foot and a thong strap over it, where vanilla
 * would draw boots — the lower legs blown up a pixel all round. Fabric's armour hook replaces the
 * boots layer for this one item.
 *
 * <p>The model is a whole biped with everything but the legs left empty, so the body's pose copies
 * across in one call ({@code copyPropertiesTo}), the way vanilla's armour models take it, and a baby
 * wearing them is scaled as its boots would be.
 */
public final class FlipFlopsRenderer implements ArmorRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "textures/entity/equipment/flip_flops.png");

    private HumanoidModel<LivingEntity> model;

    /** Leg space: the leg is 4×12×4 from y 0 at the hip, so the foot's underside is y 12. */
    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        for (String empty : new String[]{PartNames.HEAD, PartNames.HAT, PartNames.BODY,
                PartNames.RIGHT_ARM, PartNames.LEFT_ARM}) {
            root.addOrReplaceChild(empty, CubeListBuilder.create(), PartPose.ZERO);
        }
        CubeListBuilder foot = CubeListBuilder.create()
                // The sole: half a pixel under the foot, a pixel past the toes and the heel.
                .texOffs(0, 0).addBox(-2.5F, 11.5F, -3.0F, 5.0F, 1.0F, 6.0F, CubeDeformation.NONE)
                // The strap: a shell round the bottom of the foot, cut out to a V by the texture, and
                // outside the leggings' 0.5 so a pair of trousers does not swallow it.
                .texOffs(0, 8).addBox(-2.0F, 9.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.75F));
        root.addOrReplaceChild(PartNames.RIGHT_LEG, foot, PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_LEG, foot, PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(data, 32, 16);
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack,
                       LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel) {
        if (model == null) {
            // The layer is baked with the entity models, after this renderer is registered.
            model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels()
                    .bakeLayer(ModEntityModelLayers.FLIP_FLOPS));
        }
        contextModel.copyPropertiesTo(model);
        model.setAllVisible(false);
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model, TEXTURE);
    }
}
