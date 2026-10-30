package com.warlonmhite.hempdustry.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.client.render.FlipFlopsModel;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * {@link FlipFlopsModel} through Fabric's armour hook, which replaces the boots layer for this item.
 * The hook hands over the wearer's body model; a baby's pair is posed by vanilla's baby armour
 * instead, as NeoForge's hook poses it and as vanilla poses a baby's boots.
 */
public final class FlipFlopsRenderer implements ArmorRenderer {
    private final Model<Unit> model;
    private final Model<Unit> babyModel;
    private final HumanoidModel<HumanoidRenderState> babyLegs;

    public FlipFlopsRenderer(EntityRendererProvider.Context context) {
        this.model = FlipFlopsModel.create(context);
        this.babyModel = FlipFlopsModel.createBaby(context);
        this.babyLegs = FlipFlopsModel.babyArmorLegs(context);
    }

    @Override
    public void render(PoseStack matrices, SubmitNodeCollector queue, ItemStack stack,
                       HumanoidRenderState state, EquipmentSlot slot, int light,
                       HumanoidModel<HumanoidRenderState> contextModel) {
        boolean baby = FlipFlopsModel.wornByABaby(state);
        Model<Unit> model = baby ? babyModel : this.model;
        HumanoidModel<HumanoidRenderState> legs = baby ? babyLegs : contextModel;
        ArmorRenderer.submitTransformCopyingModel(legs, state, model, Unit.INSTANCE, false, queue, matrices,
                RenderTypes.armorCutoutNoCull(FlipFlopsModel.TEXTURE), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (stack.hasFoil()) {
            ArmorRenderer.submitTransformCopyingModel(legs, state, model, Unit.INSTANCE, false, queue, matrices,
                    RenderTypes.armorEntityGlint(), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        }
    }
}
