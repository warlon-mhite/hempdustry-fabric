package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

/**
 * Flip-flops drawn as flip-flops: a sole under each foot and a thong strap over it, where vanilla
 * would draw boots — the lower legs blown up a pixel all round. Fabric's armour hook replaces the
 * boots layer for this one item. The two parts are named after the body's legs, so they copy the
 * legs' transforms and walk with them.
 */
public final class FlipFlopsRenderer implements ArmorRenderer {
    private static final Identifier TEXTURE =
            Identifier.of(Hempdustry.MOD_ID, "textures/entity/equipment/flip_flops.png");

    private final Model<Unit> model;

    public FlipFlopsRenderer(EntityRendererFactory.Context context) {
        this.model = new Model<>(context.getPart(ModEntityModelLayers.FLIP_FLOPS), RenderLayers::armorCutoutNoCull) {
        };
    }

    /** Leg space: the leg is 4×12×4 from y 0 at the hip, so the foot's underside is y 12. */
    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartBuilder foot = ModelPartBuilder.create()
                // The sole: half a pixel under the foot, a pixel past the toes and the heel.
                .uv(0, 0).cuboid(-2.5F, 11.5F, -3.0F, 5.0F, 1.0F, 6.0F, Dilation.NONE)
                // The strap: a shell round the bottom of the foot, cut out to a V by the texture, and
                // outside the leggings' 0.5 so a pair of trousers does not swallow it.
                .uv(0, 8).cuboid(-2.0F, 9.5F, -2.0F, 4.0F, 2.0F, 4.0F, new Dilation(0.75F));
        data.getRoot().addChild(EntityModelPartNames.RIGHT_LEG, foot, ModelTransform.origin(-1.9F, 12.0F, 0.0F));
        data.getRoot().addChild(EntityModelPartNames.LEFT_LEG, foot, ModelTransform.origin(1.9F, 12.0F, 0.0F));
        return TexturedModelData.of(data, 32, 16);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemStack stack,
                       BipedEntityRenderState state, EquipmentSlot slot, int light,
                       BipedEntityModel<BipedEntityRenderState> contextModel) {
        ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, Unit.INSTANCE, false, queue, matrices,
                RenderLayers.armorCutoutNoCull(TEXTURE), light, OverlayTexture.DEFAULT_UV, state.outlineColor, null);
        if (stack.hasGlint()) {
            ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, Unit.INSTANCE, false, queue, matrices,
                    RenderLayers.armorEntityGlint(), light, OverlayTexture.DEFAULT_UV, state.outlineColor, null);
        }
    }
}
