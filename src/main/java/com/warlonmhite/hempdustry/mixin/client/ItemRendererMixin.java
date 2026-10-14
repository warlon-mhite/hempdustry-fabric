package com.warlonmhite.hempdustry.mixin.client;

import com.warlonmhite.hempdustry.item.ModItemProperties;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Holds a bong in 3D: the placed block's model in the hand, on the head and wherever else an item is
 * drawn as an object, and the flat sprite in the inventory, on the ground and in an item frame.
 *
 * <p>That is vanilla's own split for the trident and the spyglass, and on this line vanilla does it
 * in code, by item, inside {@code ItemRenderer} — there is no data that can say "this model in the
 * hand". So the bong joins it here. Unlike the trident, the swap is made on the way <em>into</em>
 * {@code renderItem} rather than in {@code getModel}: the GUI decides its lighting from the model
 * {@code getModel} returns, and a flat sprite lit as a block would draw dark.
 *
 * <p>The hand model carries the same {@code hempdustry:packed} override as the flat one, so a packed
 * bong shows its load, tinted by the strain, in the hand too.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("HEAD"), argsOnly = true)
    private BakedModel hempdustry$holdBongsIn3d(BakedModel model, ItemStack stack, ItemDisplayContext mode) {
        if (mode == ItemDisplayContext.GUI || mode == ItemDisplayContext.GROUND
                || mode == ItemDisplayContext.FIXED
                || !(stack.getItem() instanceof SmokingDeviceItem device) || device.placedBlock() == null) {
            return model;
        }
        BakedModel inHand = Minecraft.getInstance().getModelManager()
                .getModel(ModItemProperties.inHandModel(stack.getItem()));
        if (inHand == null) {
            return model;
        }
        BakedModel resolved = inHand.getOverrides().resolve(inHand, stack, null, null, 0);
        return resolved != null ? resolved : inHand;
    }
}
