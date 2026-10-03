package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Flip-flops drawn as flip-flops: a sole under each foot and a thong strap over it, where vanilla
 * would draw boots — the lower legs blown up a pixel all round. Fabric's armour hook replaces the
 * boots layer for this one item.
 *
 * <p>The model is a whole biped with everything but the legs left empty, so the body's pose copies
 * across in one call ({@code copyBipedStateTo}), the way vanilla's armour models take it, and a baby
 * wearing them is scaled as its boots would be.
 */
public final class FlipFlopsRenderer implements ArmorRenderer {
    private static final Identifier TEXTURE =
            Identifier.of(Hempdustry.MOD_ID, "textures/entity/equipment/flip_flops.png");

    private BipedEntityModel<LivingEntity> model;

    /** Leg space: the leg is 4×12×4 from y 0 at the hip, so the foot's underside is y 12. */
    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        for (String empty : new String[]{EntityModelPartNames.HEAD, EntityModelPartNames.HAT, EntityModelPartNames.BODY,
                EntityModelPartNames.RIGHT_ARM, EntityModelPartNames.LEFT_ARM}) {
            root.addChild(empty, ModelPartBuilder.create(), ModelTransform.NONE);
        }
        ModelPartBuilder foot = ModelPartBuilder.create()
                // The sole: half a pixel under the foot, a pixel past the toes and the heel.
                .uv(0, 0).cuboid(-2.5F, 11.5F, -3.0F, 5.0F, 1.0F, 6.0F, Dilation.NONE)
                // The strap: a shell round the bottom of the foot, cut out to a V by the texture, and
                // outside the leggings' 0.5 so a pair of trousers does not swallow it.
                .uv(0, 8).cuboid(-2.0F, 9.5F, -2.0F, 4.0F, 2.0F, 4.0F, new Dilation(0.75F));
        root.addChild(EntityModelPartNames.RIGHT_LEG, foot, ModelTransform.pivot(-1.9F, 12.0F, 0.0F));
        root.addChild(EntityModelPartNames.LEFT_LEG, foot, ModelTransform.pivot(1.9F, 12.0F, 0.0F));
        return TexturedModelData.of(data, 32, 16);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
                       LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> contextModel) {
        if (model == null) {
            // The layer is baked with the entity models, after this renderer is registered.
            model = new BipedEntityModel<>(MinecraftClient.getInstance().getEntityModelLoader()
                    .getModelPart(ModEntityModelLayers.FLIP_FLOPS));
        }
        contextModel.copyBipedStateTo(model);
        model.setVisible(false);
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model, TEXTURE);
    }
}
