package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * The beanie, worn, as a slouchy tam: the item models {@code item/hemp_beanie_worn_band} and
 * {@code item/hemp_beanie_worn_crown}, drawn on the head the way vanilla draws a carved pumpkin.
 *
 * <p>Vanilla's head layer never draws an armour helmet as its item, so this does what it does for
 * one, call for call ({@code HeadFeatureRenderer#render}: the baby scale, the head's rotation,
 * {@link HeadFeatureRenderer#translate}, the {@code head} display transform), from Fabric's armour
 * hook in place of the helmet layer.
 *
 * <p>The tam is two models and not one because its crown is tipped back 12° about
 * the back of the band, and a model file on this version takes only 0, 22.5 and 45° (22.5° showed
 * the crown's underside beside the face). So the crown's file is drawn level and the tilt is turned
 * here, about the same origin the one-file model names.
 */
public final class HempBeanieRenderer implements ArmorRenderer {
    public static final Identifier BAND = Identifier.of(Hempdustry.MOD_ID, "item/hemp_beanie_worn_band");
    public static final Identifier CROWN = Identifier.of(Hempdustry.MOD_ID, "item/hemp_beanie_worn_crown");

    private static final float CROWN_TILT = 12.0F;

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
                       LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> contextModel) {
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel band = client.getBakedModelManager().getModel(BAND);
        BakedModel crown = client.getBakedModelManager().getModel(CROWN);
        if (band == null || crown == null) {
            return;
        }
        matrices.push();
        if (entity.isBaby() && !(entity instanceof VillagerEntity)) {
            matrices.translate(0.0F, 0.03125F, 0.0F);
            matrices.scale(0.7F, 0.7F, 0.7F);
            matrices.translate(0.0F, 1.0F, 0.0F);
        }
        contextModel.getHead().rotate(matrices);
        HeadFeatureRenderer.translate(matrices, entity instanceof VillagerEntity || entity instanceof ZombieVillagerEntity);

        // ItemRenderer#renderItem's own steps for a plain model: the display transform, then the
        // model's block-space origin to its centre.
        band.getTransformation().getTransformation(ModelTransformationMode.HEAD).apply(false, matrices);
        matrices.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer buffer = ItemRenderer.getItemGlintConsumer(vertexConsumers,
                RenderLayers.getItemLayer(stack, true), true, stack.hasGlint());
        quads(band, matrices, buffer, light);
        matrices.translate(8.0F / 16.0F, 12.0F / 16.0F, 3.0F / 16.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(CROWN_TILT));
        matrices.translate(-8.0F / 16.0F, -12.0F / 16.0F, -3.0F / 16.0F);
        quads(crown, matrices, buffer, light);
        matrices.pop();
    }

    private static void quads(BakedModel model, MatrixStack matrices, VertexConsumer buffer, int light) {
        Random random = Random.create();
        for (Direction side : Direction.values()) {
            draw(model.getQuads(null, side, random), matrices, buffer, light);
        }
        draw(model.getQuads(null, null, random), matrices, buffer, light);
    }

    private static void draw(List<BakedQuad> quads, MatrixStack matrices, VertexConsumer buffer, int light) {
        for (BakedQuad quad : quads) {
            buffer.quad(matrices.peek(), quad, 1.0F, 1.0F, 1.0F, 1.0F, light, OverlayTexture.DEFAULT_UV);
        }
    }
}
