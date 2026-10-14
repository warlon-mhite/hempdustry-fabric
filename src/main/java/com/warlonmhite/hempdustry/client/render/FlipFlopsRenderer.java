package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Flip-flops drawn as flip-flops: a sole under each foot and a thong strap over it, where vanilla
 * would draw boots — the lower legs blown up a pixel all round. Fabric's armour hook replaces the
 * boots layer for this one item. The two parts are named after the body's legs, so they copy the
 * legs' transforms and walk with them.
 */
public final class FlipFlopsRenderer implements ArmorRenderer {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "textures/entity/equipment/flip_flops.png");

    private final Model<Unit> model;

    public FlipFlopsRenderer(EntityRendererProvider.Context context) {
        this.model = new Model<>(context.bakeLayer(ModEntityModelLayers.FLIP_FLOPS), RenderTypes::armorCutoutNoCull) {
        };
    }

    /** Leg space: the leg is 4×12×4 from y 0 at the hip, so the foot's underside is y 12. */
    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        CubeListBuilder foot = CubeListBuilder.create()
                // The sole: half a pixel under the foot, a pixel past the toes and the heel.
                .texOffs(0, 0).addBox(-2.5F, 11.5F, -3.0F, 5.0F, 1.0F, 6.0F, CubeDeformation.NONE)
                // The strap: a shell round the bottom of the foot, cut out to a V by the texture, and
                // outside the leggings' 0.5 so a pair of trousers does not swallow it.
                .texOffs(0, 8).addBox(-2.0F, 9.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.75F));
        data.getRoot().addOrReplaceChild(PartNames.RIGHT_LEG, foot, PartPose.offset(-1.9F, 12.0F, 0.0F));
        data.getRoot().addOrReplaceChild(PartNames.LEFT_LEG, foot, PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(data, 32, 16);
    }

    @Override
    public void render(PoseStack matrices, SubmitNodeCollector queue, ItemStack stack,
                       HumanoidRenderState state, EquipmentSlot slot, int light,
                       HumanoidModel<HumanoidRenderState> contextModel) {
        ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, Unit.INSTANCE, false, queue, matrices,
                RenderTypes.armorCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (stack.hasFoil()) {
            ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, Unit.INSTANCE, false, queue, matrices,
                    RenderTypes.armorEntityGlint(), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        }
    }
}
