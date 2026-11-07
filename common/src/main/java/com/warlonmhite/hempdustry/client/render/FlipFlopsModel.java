package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Flip-flops as flip-flops: a sole under each foot and a thong strap over it, where vanilla would
 * draw boots — the lower legs blown up a pixel all round. The loader draws this model in place of
 * the boots layer for this one item, through its own armour-rendering hook.
 *
 * <p>The model is a whole biped with everything but the legs hidden, so the body's pose copies
 * across in one call ({@code copyPropertiesTo}), the way vanilla's armour models take it, and a baby
 * wearing them is scaled as its boots would be. 1.16.5 has no model layers: the legs are built here,
 * in place of the biped's own.
 */
public final class FlipFlopsModel extends HumanoidModel<LivingEntity> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(Hempdustry.MOD_ID, "textures/entity/equipment/flip_flops.png");

    /** Leg space: the leg is 4×12×4 from y 0 at the hip, so the foot's underside is y 12. */
    public FlipFlopsModel() {
        super(0.0F, 0.0F, 32, 16);
        this.rightLeg = foot(-1.9F);
        this.leftLeg = foot(1.9F);
    }

    private ModelPart foot(float x) {
        ModelPart foot = new ModelPart(this);
        // The sole: half a pixel under the foot, a pixel past the toes and the heel.
        foot.texOffs(0, 0).addBox(-2.5F, 11.5F, -3.0F, 5.0F, 1.0F, 6.0F, 0.0F);
        // The strap: a shell round the bottom of the foot, cut out to a V by the texture, and
        // outside the leggings' 0.5 so a pair of trousers does not swallow it.
        foot.texOffs(0, 8).addBox(-2.0F, 9.5F, -2.0F, 4.0F, 2.0F, 4.0F, 0.75F);
        foot.setPos(x, 12.0F, 0.0F);
        return foot;
    }

    /** Poses {@code flipFlops} as {@code wearer} stands, showing only the feet. */
    public static void pose(HumanoidModel<LivingEntity> wearer, HumanoidModel<LivingEntity> flipFlops) {
        wearer.copyPropertiesTo(flipFlops);
        flipFlops.setAllVisible(false);
        flipFlops.rightLeg.visible = true;
        flipFlops.leftLeg.visible = true;
    }
}
