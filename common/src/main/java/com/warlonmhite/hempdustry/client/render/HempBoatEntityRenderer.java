package com.warlonmhite.hempdustry.client.render;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Reimplementation of vanilla's {@code BoatRenderer}: the geometry (a {@code ModelPart} borrowed from
 * vanilla's OAK boat model layer, which is identical across wood types) is unchanged, only the bound texture
 * is swapped for a single fixed hemp texture instead of vanilla's per-{@code Boat.Type} lookup map.
 */
public class HempBoatEntityRenderer extends EntityRenderer<Boat> {
    private final ResourceLocation texture;
    private final BoatModel model;

    public HempBoatEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.8F;
        this.texture = new ResourceLocation(Hempdustry.MOD_ID, "textures/entity/boat/hemp.png");
        ModelPart modelPart = ctx.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK));
        this.model = new BoatModel(modelPart);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat boatEntity) {
        return this.texture;
    }

    @Override
    public void render(Boat boatEntity, float yaw, float tickDelta, PoseStack matrixStack,
                        MultiBufferSource vertexConsumerProvider, int light) {
        matrixStack.pushPose();
        matrixStack.translate(0.0F, 0.375F, 0.0F);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - yaw));
        float wobbleTicks = (float) boatEntity.getHurtTime() - tickDelta;
        float wobbleStrength = boatEntity.getDamage() - tickDelta;
        if (wobbleStrength < 0.0F) {
            wobbleStrength = 0.0F;
        }
        if (wobbleTicks > 0.0F) {
            matrixStack.mulPose(Vector3f.XP.rotationDegrees(
                    Mth.sin(wobbleTicks) * wobbleTicks * wobbleStrength / 10.0F * (float) boatEntity.getHurtDir()));
        }

        float bubbleWobble = boatEntity.getBubbleAngle(tickDelta);
        if (!Mth.equal(bubbleWobble, 0.0F)) {
            matrixStack.mulPose(new Quaternion(new Vector3f(1.0F, 0.0F, 1.0F), bubbleWobble, true));
        }

        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));
        this.model.setupAnim(boatEntity, tickDelta, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.model.renderType(this.texture));
        this.model.renderToBuffer(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        if (!boatEntity.isUnderWater()) {
            VertexConsumer waterConsumer = vertexConsumerProvider.getBuffer(RenderType.waterMask());
            this.model.waterPatch().render(matrixStack, waterConsumer, light, OverlayTexture.NO_OVERLAY);
        }

        matrixStack.popPose();
        super.render(boatEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
}
