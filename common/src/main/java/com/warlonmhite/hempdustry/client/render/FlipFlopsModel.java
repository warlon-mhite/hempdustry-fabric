package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/**
 * Flip-flops as flip-flops: a sole under each foot and a thong strap over it, where vanilla would
 * draw boots — the lower legs blown up a pixel all round. The two parts are named after the body's
 * legs, so they copy the legs' transforms and walk with them. Each loader draws this model in place
 * of the boots layer for this one item, through its own armour-rendering hook.
 */
public final class FlipFlopsModel {
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "textures/entity/equipment/flip_flops.png");

    private FlipFlopsModel() {
    }

    public static Model<Unit> create(EntityRendererProvider.Context context) {
        return create(context.bakeLayer(ModEntityModelLayers.FLIP_FLOPS));
    }

    /** The model around {@code root}, the {@link ModEntityModelLayers#FLIP_FLOPS} layer once baked. */
    public static Model<Unit> create(ModelPart root) {
        return new Model<>(root, RenderTypes::armorCutoutNoCull) {
        };
    }

    /** Leg space: the leg is 4×12×4 from y 0 at the hip, so the foot's underside is y 12. */
    public static LayerDefinition createLayer() {
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
}
