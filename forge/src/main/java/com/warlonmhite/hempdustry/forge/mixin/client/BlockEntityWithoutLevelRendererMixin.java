package com.warlonmhite.hempdustry.forge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.client.render.HempBedBlockEntityRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The hemp bed's item, drawn with the bed's own renderer. Vanilla draws a bed item from the bed's
 * {@code DyeColor}, which the hemp bed has none of, so it would draw a vanilla bed; Fabric's built-in
 * item hook takes it by the item, and Forge 36 would have the item name its own renderer
 * ({@code Item.Properties#setISTER}); the bed item is common code, so it names none and this draws it.
 */
@Mixin(BlockEntityWithoutLevelRenderer.class)
public abstract class BlockEntityWithoutLevelRendererMixin {
    @Inject(method = "renderByItem", at = @At("HEAD"), cancellable = true)
    private void hempdustry$drawHempBed(ItemStack stack, ItemTransforms.TransformType context, PoseStack matrices,
                                        MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (stack.getItem() == ModBlocks.HEMP_BED.asItem()) {
            HempBedBlockEntityRenderer.renderItem(matrices, buffers, light, overlay);
            ci.cancel();
        }
    }
}
