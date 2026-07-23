package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BoatEntityModel;
import net.minecraft.client.render.entity.model.ChestBoatEntityModel;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ModelWithWaterPatch;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

/**
 * Reimplementation of vanilla's {@code BoatEntityRenderer}: the geometry (a {@code ModelPart} borrowed from
 * vanilla's OAK boat model layer, which is identical across wood types) is unchanged, only the bound texture
 * is swapped for a single fixed hemp texture instead of vanilla's per-{@code BoatEntity.Type} lookup map.
 */
public class HempBoatEntityRenderer extends EntityRenderer<BoatEntity> {
    private final Identifier texture;
    private final CompositeEntityModel<BoatEntity> model;

    public HempBoatEntityRenderer(EntityRendererFactory.Context ctx, boolean chest) {
        super(ctx);
        this.shadowRadius = 0.8F;
        this.texture = Identifier.of(Hempdustry.MOD_ID,
                chest ? "textures/entity/chest_boat/hemp.png" : "textures/entity/boat/hemp.png");
        ModelPart modelPart = ctx.getPart(chest
                ? EntityModelLayers.createChestBoat(BoatEntity.Type.OAK)
                : EntityModelLayers.createBoat(BoatEntity.Type.OAK));
        this.model = chest ? new ChestBoatEntityModel(modelPart) : new BoatEntityModel(modelPart);
    }

    @Override
    public Identifier getTexture(BoatEntity boatEntity) {
        return this.texture;
    }

    @Override
    public void render(BoatEntity boatEntity, float yaw, float tickDelta, MatrixStack matrixStack,
                        VertexConsumerProvider vertexConsumerProvider, int light) {
        matrixStack.push();
        matrixStack.translate(0.0F, 0.375F, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
        float wobbleTicks = (float) boatEntity.getDamageWobbleTicks() - tickDelta;
        float wobbleStrength = boatEntity.getDamageWobbleStrength() - tickDelta;
        if (wobbleStrength < 0.0F) {
            wobbleStrength = 0.0F;
        }
        if (wobbleTicks > 0.0F) {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(wobbleTicks) * wobbleTicks * wobbleStrength / 10.0F * (float) boatEntity.getDamageWobbleSide()));
        }

        float bubbleWobble = boatEntity.interpolateBubbleWobble(tickDelta);
        if (!MathHelper.approximatelyEquals(bubbleWobble, 0.0F)) {
            matrixStack.multiply(new Quaternionf().setAngleAxis(bubbleWobble * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }

        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        this.model.setAngles(boatEntity, tickDelta, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.model.getLayer(this.texture));
        this.model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        if (!boatEntity.isSubmergedInWater()) {
            VertexConsumer waterConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getWaterMask());
            if (this.model instanceof ModelWithWaterPatch modelWithWaterPatch) {
                modelWithWaterPatch.getWaterPatch().render(matrixStack, waterConsumer, light, OverlayTexture.DEFAULT_UV);
            }
        }

        matrixStack.pop();
        super.render(boatEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
}
