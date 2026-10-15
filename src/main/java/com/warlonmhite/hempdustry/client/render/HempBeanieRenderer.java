package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.client.ClientServices;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * The beanie, worn, as a slouchy tam: the item models {@code item/hemp_beanie_worn_band} and
 * {@code item/hemp_beanie_worn_crown}, drawn on the head the way vanilla draws a carved pumpkin.
 *
 * <p>Vanilla's head layer never draws an armour helmet as its item, so this does what it does for
 * one, call for call ({@code CustomHeadLayer#render}: the baby scale, the head's rotation,
 * {@link CustomHeadLayer#translateToHead}, the {@code head} display transform), from each loader's
 * armour hook in place of the helmet layer.
 *
 * <p>The tam is two models and not one because its crown is tipped back 12° about
 * the back of the band, and a model file on this version takes only 0, 22.5 and 45° (22.5° showed
 * the crown's underside beside the face). So the crown's file is drawn level and the tilt is turned
 * here, about the same origin the one-file model names.
 */
public final class HempBeanieRenderer {
    public static final ResourceLocation BAND = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/hemp_beanie_worn_band");
    public static final ResourceLocation CROWN = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/hemp_beanie_worn_crown");

    private static final float CROWN_TILT = 12.0F;

    private HempBeanieRenderer() {
    }

    /** The tam on {@code entity}'s head, as {@code contextModel} holds it. */
    public static void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack,
                              LivingEntity entity, int light, HumanoidModel<?> contextModel) {
        BakedModel band = ClientServices.CLIENT.extraModel(BAND);
        BakedModel crown = ClientServices.CLIENT.extraModel(CROWN);
        if (band == null || crown == null) {
            return;
        }
        matrices.pushPose();
        if (entity.isBaby() && !(entity instanceof Villager)) {
            matrices.translate(0.0F, 0.03125F, 0.0F);
            matrices.scale(0.7F, 0.7F, 0.7F);
            matrices.translate(0.0F, 1.0F, 0.0F);
        }
        contextModel.getHead().translateAndRotate(matrices);
        CustomHeadLayer.translateToHead(matrices, entity instanceof Villager || entity instanceof ZombieVillager);

        // ItemRenderer#renderItem's own steps for a plain model: the display transform, then the
        // model's block-space origin to its centre.
        band.getTransforms().getTransform(ItemDisplayContext.HEAD).apply(false, matrices);
        matrices.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer buffer = ItemRenderer.getFoilBuffer(vertexConsumers,
                ItemBlockRenderTypes.getRenderType(stack, true), true, stack.hasFoil());
        quads(band, matrices, buffer, light);
        matrices.translate(8.0F / 16.0F, 12.0F / 16.0F, 3.0F / 16.0F);
        matrices.mulPose(Axis.XP.rotationDegrees(CROWN_TILT));
        matrices.translate(-8.0F / 16.0F, -12.0F / 16.0F, -3.0F / 16.0F);
        quads(crown, matrices, buffer, light);
        matrices.popPose();
    }

    private static void quads(BakedModel model, PoseStack matrices, VertexConsumer buffer, int light) {
        RandomSource random = RandomSource.create();
        for (Direction side : Direction.values()) {
            draw(model.getQuads(null, side, random), matrices, buffer, light);
        }
        draw(model.getQuads(null, null, random), matrices, buffer, light);
    }

    private static void draw(List<BakedQuad> quads, PoseStack matrices, VertexConsumer buffer, int light) {
        for (BakedQuad quad : quads) {
            buffer.putBulkData(matrices.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY);
        }
    }
}
