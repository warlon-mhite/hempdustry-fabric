package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Flip-flops as flip-flops: a sole under each foot and a thong strap over it, where vanilla would
 * draw boots — the lower legs blown up a pixel all round. Each loader draws this model in place of
 * the boots layer for this one item, through its own armour-rendering hook.
 *
 * <p>The model is a whole biped with everything but the legs left empty, so the body's pose copies
 * across in one call ({@code copyPropertiesTo}), the way vanilla's armour models take it, and a baby
 * wearing them is scaled as its boots would be.
 */
public final class FlipFlopsModel {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "textures/entity/equipment/flip_flops.png");

    private FlipFlopsModel() {
    }

    /** The model, baked from its layer. */
    public static HumanoidModel<LivingEntity> create(EntityModelSet models) {
        return new HumanoidModel<>(models.bakeLayer(ModEntityModelLayers.FLIP_FLOPS));
    }

    /** Poses {@code flipFlops} as {@code wearer} stands, showing only the feet. */
    public static void pose(HumanoidModel<LivingEntity> wearer, HumanoidModel<LivingEntity> flipFlops) {
        wearer.copyPropertiesTo(flipFlops);
        flipFlops.setAllVisible(false);
        flipFlops.rightLeg.visible = true;
        flipFlops.leftLeg.visible = true;
    }

    /** Leg space: the leg is 4×12×4 from y 0 at the hip, so the foot's underside is y 12. */
    public static LayerDefinition createLayer() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        for (String empty : new String[]{PartNames.HEAD, PartNames.HAT, PartNames.BODY,
                PartNames.RIGHT_ARM, PartNames.LEFT_ARM}) {
            root.addOrReplaceChild(empty, CubeListBuilder.create(), PartPose.ZERO);
        }
        CubeListBuilder foot = CubeListBuilder.create()
                // The sole: half a pixel under the foot, a pixel past the toes and the heel.
                .texOffs(0, 0).addBox(-2.5F, 11.5F, -3.0F, 5.0F, 1.0F, 6.0F, CubeDeformation.NONE)
                // The strap: a shell round the bottom of the foot, cut out to a V by the texture, and
                // outside the leggings' 0.5 so a pair of trousers does not swallow it.
                .texOffs(0, 8).addBox(-2.0F, 9.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.75F));
        root.addOrReplaceChild(PartNames.RIGHT_LEG, foot, PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_LEG, foot, PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(data, 32, 16);
    }
}
