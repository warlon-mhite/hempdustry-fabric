package com.warlonmhite.hempdustry.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Llama;
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
 * {@code WoolCarpetBlock} has — so a hemp carpet went on and drew nothing, and on a trader llama drew
 * the trader's own cloth instead. When the llama wears ours, this draws it exactly as vanilla draws a
 * dyed one (same model, same render layer), from {@code textures/entity/llama/decor/hemp.png}, and
 * vanilla's code is skipped for that llama only.
 */
@Mixin(LlamaDecorLayer.class)
public abstract class LlamaDecorFeatureRendererMixin {
    @Unique
    private static final ResourceLocation HEMP_DECOR =
            ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "textures/entity/llama/decor/hemp.png");

    @Shadow
    @Final
    private LlamaModel<Llama> model;

    @SuppressWarnings("unchecked")
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/horse/Llama;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void hempdustry$drawHempCarpet(PoseStack matrices, MultiBufferSource vertexConsumers, int light,
                                           Llama llama, float limbAngle, float limbDistance, float tickDelta,
                                           float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!llama.getBodyArmorItem().is(ModBlocks.HEMP_CARPET.asItem())) {
            return;
        }
        ((RenderLayer<Llama, LlamaModel<Llama>>) (Object) this).getParentModel().copyPropertiesTo(model);
        model.setupAnim(llama, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        model.renderToBuffer(matrices, vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(HEMP_DECOR)),
                light, OverlayTexture.NO_OVERLAY);
        ci.cancel();
    }
}
