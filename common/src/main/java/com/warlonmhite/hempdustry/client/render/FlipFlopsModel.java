package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;

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

    /** The baby's pair, {@link #createBabyLayer}. */
    public static Model<Unit> createBaby(EntityRendererProvider.Context context) {
        return create(context.bakeLayer(ModEntityModelLayers.FLIP_FLOPS_BABY));
    }

    /**
     * Vanilla's baby armour for the feet, which poses the baby's pair: its legs are the same for a
     * baby zombie, husk, drowned, zombie villager and piglin, where the babies' own legs are not (a
     * baby zombie villager's are a pixel shorter).
     */
    public static HumanoidModel<HumanoidRenderState> babyArmorLegs(EntityRendererProvider.Context context) {
        return new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY_ARMOR.feet()));
    }

    /**
     * Whether {@code state} is drawn with vanilla's baby armour, which since 26.1 is a model of its
     * own rather than the grown-up's scaled down; a small armour stand still wears the grown-up's
     * ({@code HumanoidArmorLayer#renderArmorPiece}).
     */
    public static boolean wornByABaby(HumanoidRenderState state) {
        return state.isBaby && state.entityType != EntityTypes.ARMOR_STAND;
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

    /**
     * A baby's leg space, which is the leg space of vanilla's baby armour ({@code HumanoidModel#createBabyArmorMesh}),
     * the one set of legs every baby humanoid's boots hang from: hips at y 20, the foot's underside at
     * y 4 (the ground), and each foot centred half a pixel in from the hip and half a pixel forward.
     * The same pair shrunk to a baby's 2-pixel foot, with its own UVs on the right half of the
     * texture; the strap stands outside the baby leggings' 0.2.
     */
    public static LayerDefinition createBabyLayer() {
        MeshDefinition data = new MeshDefinition();
        data.getRoot().addOrReplaceChild(PartNames.RIGHT_LEG, babyFoot(0.5F), PartPose.offset(-1.5F, 20.0F, 0.5F));
        data.getRoot().addOrReplaceChild(PartNames.LEFT_LEG, babyFoot(-0.5F), PartPose.offset(1.5F, 20.0F, 0.5F));
        return LayerDefinition.create(data, 32, 16);
    }

    /** A baby's sole and strap round a foot centred at {@code x}, z −0.5. */
    private static CubeListBuilder babyFoot(float x) {
        return CubeListBuilder.create()
                .texOffs(16, 0).addBox(x - 1.5F, 3.5F, -2.0F, 3.0F, 1.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(16, 8).addBox(x - 1.0F, 1.75F, -1.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.75F, 0.0F, 0.75F));
    }
}
