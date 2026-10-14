package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.WaterPatchModel;
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
import org.joml.Quaternionf;

/**
 * Reimplementation of vanilla's {@code BoatRenderer}: the geometry (a {@code ModelPart} borrowed from
 * vanilla's OAK boat model layer, which is identical across wood types) is unchanged, only the bound texture
 * is swapped for a single fixed hemp texture instead of vanilla's per-{@code Boat.Type} lookup map.
 */
public class HempBoatEntityRenderer extends EntityRenderer<Boat> {
    private final ResourceLocation texture;
    private final ListModel<Boat> model;

    public HempBoatEntityRenderer(EntityRendererProvider.Context ctx, boolean chest) {
        super(ctx);
        this.shadowRadius = 0.8F;
        this.texture = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID,
                chest ? "textures/entity/chest_boat/hemp.png" : "textures/entity/boat/hemp.png");
        ModelPart modelPart = ctx.bakeLayer(chest
                ? ModelLayers.createChestBoatModelName(Boat.Type.OAK)
                : ModelLayers.createBoatModelName(Boat.Type.OAK));
        this.model = chest ? new ChestBoatModel(modelPart) : new BoatModel(modelPart);
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
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float wobbleTicks = (float) boatEntity.getHurtTime() - tickDelta;
        float wobbleStrength = boatEntity.getDamage() - tickDelta;
        if (wobbleStrength < 0.0F) {
            wobbleStrength = 0.0F;
        }
        if (wobbleTicks > 0.0F) {
            matrixStack.mulPose(Axis.XP.rotationDegrees(
                    Mth.sin(wobbleTicks) * wobbleTicks * wobbleStrength / 10.0F * (float) boatEntity.getHurtDir()));
        }

        float bubbleWobble = boatEntity.getBubbleAngle(tickDelta);
        if (!Mth.equal(bubbleWobble, 0.0F)) {
            matrixStack.mulPose(new Quaternionf().setAngleAxis(bubbleWobble * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }

        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        this.model.setupAnim(boatEntity, tickDelta, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.model.renderType(this.texture));
        this.model.renderToBuffer(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        if (!boatEntity.isUnderWater()) {
            VertexConsumer waterConsumer = vertexConsumerProvider.getBuffer(RenderType.waterMask());
            if (this.model instanceof WaterPatchModel modelWithWaterPatch) {
                modelWithWaterPatch.waterPatch().render(matrixStack, waterConsumer, light, OverlayTexture.NO_OVERLAY);
            }
        }

        matrixStack.popPose();
        super.render(boatEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
}
