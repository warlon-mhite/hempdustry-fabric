package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * The Hemp Press's moving parts: the iron platen, and the four-spoke capstan that screws it down the
 * rod onto the rosin packet.
 *
 * <p>Everything that stands still is the block model. These two are block models too —
 * {@code block/hemp_press_platen} and {@code block/hemp_press_screw}, authored in the same Blockbench
 * file as the frame — but no block state names them, so nothing would load them on its own. Fabric
 * API's model loading adds them by id ({@link #PARTS}, registered from {@code HempdustryClient}),
 * and this draws the baked models straight through the block model renderer. The press's own item
 * is the same parts at rest, put together as the models bake ({@link #ICON_PARTS}), so the one in
 * your hand matches the one on the floor.
 *
 * <p>Both parts are square and centred, so {@code facing} changes nothing about them and is not read.
 */
public class HempPressBlockEntityRenderer implements BlockEntityRenderer<HempPressBlockEntity> {

    private static final ResourceLocation PLATEN = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/hemp_press_platen");
    private static final ResourceLocation SCREW = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/hemp_press_screw");

    /** The models this draws, for the model loader to bake. */
    public static final List<ResourceLocation> PARTS = List.of(PLATEN, SCREW);

    /** The press's item model, drawn as {@link #ICON_PARTS} put together ({@code CompositeItemModel}). */
    public static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/hemp_press");
    /** The whole press at rest: the frame, the bed plate, then these two where the stroke starts. */
    public static final List<ResourceLocation> ICON_PARTS = List.of(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/hemp_press"),
            ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/hemp_press_plate"), PLATEN, SCREW);

    /** Pixels the platen travels: from under the crossbeam down onto the packet. */
    private static final float TRAVEL = 4.0F;
    /** Turns of the capstan over that travel — a screw press takes a lot of turning. */
    private static final float TURNS = 1.5F;
    /** Share of each squeeze spent screwing down. */
    private static final float DESCENT = 0.55F;
    /** Where the squeeze ends and the lift begins; the lift is quick, the way an unwound screw is. */
    private static final float HOLD = 0.9F;

    public HempPressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    /**
     * One squeeze over its phase 0..1: screw down, hold, lift. The item comes out at the end of the
     * phase, so by the time it lands in the output slot the platen is back up for the next one.
     */
    static float depth(float phase) {
        if (phase < DESCENT) {
            return smooth(phase / DESCENT);
        }
        if (phase < HOLD) {
            return 1.0F;
        }
        return 1.0F - smooth((phase - HOLD) / (1.0F - HOLD));
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public void render(HempPressBlockEntity press, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        float ticks = press.strokeTicks(tickDelta);
        float depth = ticks < 0 ? 0.0F : depth(ticks / HempPressBlockEntity.pressTime());

        Minecraft client = Minecraft.getInstance();
        ModelBlockRenderer renderer = client.getBlockRenderer().getModelRenderer();
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.cutout());

        matrices.pushPose();
        matrices.translate(0.0F, -depth * TRAVEL / 16.0F, 0.0F);
        draw(renderer, client.getModelManager().getModel(PLATEN), matrices, buffer, light, overlay);

        // A right-hand thread drives down turning clockwise seen from above: a negative turn about +Y,
        // through the rod at the middle of the block.
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.mulPose(Axis.YP.rotationDegrees(-depth * TURNS * 360.0F));
        matrices.translate(-0.5F, 0.0F, -0.5F);
        draw(renderer, client.getModelManager().getModel(SCREW), matrices, buffer, light, overlay);
        matrices.popPose();
    }

    private static void draw(ModelBlockRenderer renderer, BakedModel model, PoseStack matrices,
                             VertexConsumer buffer, int light, int overlay) {
        renderer.renderModel(matrices.last(), buffer, null, model, 1.0F, 1.0F, 1.0F, light, overlay);
    }
}
