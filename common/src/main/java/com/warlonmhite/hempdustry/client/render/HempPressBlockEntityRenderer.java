package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Hemp Press's moving parts: the iron platen, and the four-spoke capstan that screws it down the
 * rod onto the rosin packet.
 *
 * <p>Everything that stands still is the block model. These two are block models too —
 * {@code block/hemp_press_platen} and {@code block/hemp_press_screw}, authored in the same Blockbench
 * file as the frame — but Fabric API has no extra-model loading on 1.21.11, so there is no way to ask
 * the model manager for one by id. A client item definition is the one place vanilla looks a model
 * up by id alone, with no registered item behind it: {@code items/hemp_press_platen.json} and
 * {@code items/hemp_press_screw.json} name them, and a stick carrying an {@code item_model} component
 * pointing there is drawn through the item pipeline the way a campfire draws what it cooks. The
 * stick itself is never seen. The press's own item gets the same parts at rest through a composite
 * definition, so the one in your hand matches the one on the floor.
 *
 * <p>Both parts are square and centred, so {@code facing} changes nothing about them and is not read.
 */
public class HempPressBlockEntityRenderer
        implements BlockEntityRenderer<HempPressBlockEntity, HempPressBlockEntityRenderer.State> {

    // Templates, made into stacks only when a press is drawn: since 26.1 no ItemStack can exist before a
    // world has bound the items' components, and this class loads with the client's first resource reload.
    private static final ItemStackTemplate PLATEN = part("hemp_press_platen");
    private static final ItemStackTemplate SCREW = part("hemp_press_screw");

    /** Pixels the platen travels: from under the crossbeam down onto the packet. */
    private static final float TRAVEL = 4.0F;
    /** Turns of the capstan over that travel — a screw press takes a lot of turning. */
    private static final float TURNS = 1.5F;
    /** Share of each squeeze spent screwing down. */
    private static final float DESCENT = 0.55F;
    /** Where the squeeze ends and the lift begins; the lift is quick, the way an unwound screw is. */
    private static final float HOLD = 0.9F;

    private final ItemModelResolver itemModels;

    public HempPressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
    }

    private static ItemStackTemplate part(String model) {
        return new ItemStackTemplate(Items.STICK, DataComponentPatch.builder()
                .set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, model))
                .build());
    }

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState platen = new ItemStackRenderState();
        final ItemStackRenderState screw = new ItemStackRenderState();
        /** 0 at rest under the crossbeam, 1 bottomed out on the packet. */
        float depth;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HempPressBlockEntity press, State state, float tickDelta, Vec3 cameraPos,
                                  @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(press, state, tickDelta, cameraPos, crumblingOverlay);
        itemModels.updateForTopItem(state.platen, PLATEN.create(), ItemDisplayContext.NONE, press.getLevel(), null, 0);
        itemModels.updateForTopItem(state.screw, SCREW.create(), ItemDisplayContext.NONE, press.getLevel(), null, 0);
        float ticks = press.strokeTicks(tickDelta);
        state.depth = ticks < 0 ? 0.0F : depth(ticks / HempPressBlockEntity.pressTime());
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
    public void submit(State state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
        matrices.pushPose();
        // The item pipeline draws a model centred on the origin, so stand it in the middle of the block.
        matrices.translate(0.5F, 0.5F - state.depth * TRAVEL / 16.0F, 0.5F);
        state.platen.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        // A right-hand thread drives down turning clockwise seen from above: a negative turn about +Y.
        matrices.mulPose(Axis.YP.rotationDegrees(-state.depth * TURNS * 360.0F));
        state.screw.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();
    }
}
