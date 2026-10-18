package com.warlonmhite.hempdustry.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.client.render.HempBedBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The hemp bed's item, drawn with the bed's own renderer: NeoForge asks the item for its built-in
 * renderer, where Fabric's built-in item hook takes it by the item.
 */
final class HempBedItemExtensions implements IClientItemExtensions {
    private BlockEntityWithoutLevelRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            Minecraft client = Minecraft.getInstance();
            renderer = new BlockEntityWithoutLevelRenderer(client.getBlockEntityRenderDispatcher(), client.getEntityModels()) {
                @Override
                public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack matrices,
                                         MultiBufferSource vertexConsumers, int light, int overlay) {
                    HempBedBlockEntityRenderer.renderItem(matrices, vertexConsumers, light, overlay);
                }
            };
        }
        return renderer;
    }
}
