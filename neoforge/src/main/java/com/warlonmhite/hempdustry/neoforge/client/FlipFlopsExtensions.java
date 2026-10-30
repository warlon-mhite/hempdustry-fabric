package com.warlonmhite.hempdustry.neoforge.client;

import com.warlonmhite.hempdustry.client.render.FlipFlopsModel;
import com.warlonmhite.hempdustry.client.render.ModEntityModelLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link FlipFlopsModel} through NeoForge's armour hook, in place of the boots layer: drawn once,
 * untinted, with its own texture, where the outfit's equipment asset would draw two layers of boots.
 *
 * <p>NeoForge poses an armour model with the wearer's state when it draws it, so the flip-flops are
 * one model per body model they stand in for, which poses that body model and takes its legs' pose --
 * what Fabric's hook does for the same item. On a baby the body model is vanilla's baby armour, which
 * the baby's pair is drawn for.
 */
final class FlipFlopsExtensions implements IClientItemExtensions {
    private final Map<HumanoidModel<?>, OnLegs> models = new WeakHashMap<>();
    private EntityModelSet bakedFrom;

    @Override
    @SuppressWarnings("rawtypes")
    public synchronized Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        if (!(original instanceof HumanoidModel<?> body)) {
            return original;
        }
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (models != bakedFrom) {
            // A resource reload bakes every layer again; the old parts would draw the old pack.
            this.models.clear();
            bakedFrom = models;
        }
        boolean baby = layerType == EquipmentClientInfo.LayerType.HUMANOID_BABY;
        return this.models.computeIfAbsent(body, b -> new OnLegs(
                models.bakeLayer(baby ? ModEntityModelLayers.FLIP_FLOPS_BABY : ModEntityModelLayers.FLIP_FLOPS), b));
    }

    @Override
    public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier fallback) {
        return FlipFlopsModel.TEXTURE;
    }

    @Override
    public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int fallbackColor) {
        // The first layer only, as drawn; 0 skips a layer.
        return layerIdx == 0 ? 0xFFFFFFFF : 0;
    }

    /** The flip-flops, posed by the legs of {@code body}. */
    private static final class OnLegs extends Model<HumanoidRenderState> {
        private final HumanoidModel<HumanoidRenderState> body;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        @SuppressWarnings("unchecked")
        OnLegs(ModelPart root, HumanoidModel<?> body) {
            super(root, RenderTypes::armorCutoutNoCull);
            this.body = (HumanoidModel<HumanoidRenderState>) body;
            this.rightLeg = root.getChild(PartNames.RIGHT_LEG);
            this.leftLeg = root.getChild(PartNames.LEFT_LEG);
        }

        @Override
        public void setupAnim(HumanoidRenderState state) {
            super.setupAnim(state);
            body.setupAnim(state);
            rightLeg.loadPose(body.rightLeg.storePose());
            leftLeg.loadPose(body.leftLeg.storePose());
        }
    }
}
