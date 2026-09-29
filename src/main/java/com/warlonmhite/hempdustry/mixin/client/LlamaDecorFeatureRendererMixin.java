package com.warlonmhite.hempdustry.mixin.client;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.LlamaDecorFeatureRenderer;
import net.minecraft.client.render.entity.model.LlamaEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the hemp carpet on a llama. A llama takes any carpet in {@code #minecraft:wool_carpets}, but
 * this renderer picks the carpet's texture from its {@code DyeColor}, which only a
 * {@code DyedCarpetBlock} has — so a hemp carpet went on and drew nothing, and on a trader llama drew
 * the trader's own cloth instead. When the llama wears ours, this draws it exactly as vanilla draws a
 * dyed one (same model, same render layer), from {@code textures/entity/llama/decor/hemp.png}, and
 * vanilla's code is skipped for that llama only.
 */
@Mixin(LlamaDecorFeatureRenderer.class)
public abstract class LlamaDecorFeatureRendererMixin {
    @Unique
    private static final Identifier HEMP_DECOR =
            Identifier.of(Hempdustry.MOD_ID, "textures/entity/llama/decor/hemp.png");

    @Shadow
    @Final
    private LlamaEntityModel<LlamaEntity> model;

    @SuppressWarnings("unchecked")
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/passive/LlamaEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void hempdustry$drawHempCarpet(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                           LlamaEntity llama, float limbAngle, float limbDistance, float tickDelta,
                                           float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!llama.getBodyArmor().isOf(ModBlocks.HEMP_CARPET.asItem())) {
            return;
        }
        ((FeatureRenderer<LlamaEntity, LlamaEntityModel<LlamaEntity>>) (Object) this).getContextModel().copyStateTo(model);
        model.setAngles(llama, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        model.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(HEMP_DECOR)),
                light, OverlayTexture.DEFAULT_UV);
        ci.cancel();
    }
}
