package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

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

    private static final Identifier PLATEN = Identifier.of(Hempdustry.MOD_ID, "block/hemp_press_platen");
    private static final Identifier SCREW = Identifier.of(Hempdustry.MOD_ID, "block/hemp_press_screw");

    /** The models this draws, for the model loader to bake. */
    public static final List<Identifier> PARTS = List.of(PLATEN, SCREW);

    /** The press's item model, drawn as {@link #ICON_PARTS} put together ({@code CompositeItemModel}). */
    public static final Identifier ICON = Identifier.of(Hempdustry.MOD_ID, "item/hemp_press");
    /** The whole press at rest: the frame, the bed plate, then these two where the stroke starts. */
    public static final List<Identifier> ICON_PARTS = List.of(Identifier.of(Hempdustry.MOD_ID, "block/hemp_press"),
            Identifier.of(Hempdustry.MOD_ID, "block/hemp_press_plate"), PLATEN, SCREW);

    /** Pixels the platen travels: from under the crossbeam down onto the packet. */
    private static final float TRAVEL = 4.0F;
    /** Turns of the capstan over that travel — a screw press takes a lot of turning. */
    private static final float TURNS = 1.5F;
    /** Share of each squeeze spent screwing down. */
    private static final float DESCENT = 0.55F;
    /** Where the squeeze ends and the lift begins; the lift is quick, the way an unwound screw is. */
    private static final float HOLD = 0.9F;

    public HempPressBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
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
    public void render(HempPressBlockEntity press, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        float ticks = press.strokeTicks(tickDelta);
        float depth = ticks < 0 ? 0.0F : depth(ticks / HempPressBlockEntity.pressTime());

        MinecraftClient client = MinecraftClient.getInstance();
        BlockModelRenderer renderer = client.getBlockRenderManager().getModelRenderer();
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutout());

        matrices.push();
        matrices.translate(0.0F, -depth * TRAVEL / 16.0F, 0.0F);
        draw(renderer, client.getBakedModelManager().getModel(PLATEN), matrices, buffer, light, overlay);

        // A right-hand thread drives down turning clockwise seen from above: a negative turn about +Y,
        // through the rod at the middle of the block.
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-depth * TURNS * 360.0F));
        matrices.translate(-0.5F, 0.0F, -0.5F);
        draw(renderer, client.getBakedModelManager().getModel(SCREW), matrices, buffer, light, overlay);
        matrices.pop();
    }

    private static void draw(BlockModelRenderer renderer, BakedModel model, MatrixStack matrices,
                             VertexConsumer buffer, int light, int overlay) {
        renderer.render(matrices.peek(), buffer, null, model, 1.0F, 1.0F, 1.0F, light, overlay);
    }
}
