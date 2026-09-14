package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
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

    private static final ItemStack PLATEN = part("hemp_press_platen");
    private static final ItemStack SCREW = part("hemp_press_screw");

    /** Pixels the platen travels: from under the crossbeam down onto the packet. */
    private static final float TRAVEL = 4.0F;
    /** Turns of the capstan over that travel — a screw press takes a lot of turning. */
    private static final float TURNS = 1.5F;
    /** Share of each squeeze spent screwing down. */
    private static final float DESCENT = 0.55F;
    /** Where the squeeze ends and the lift begins; the lift is quick, the way an unwound screw is. */
    private static final float HOLD = 0.9F;

    private final ItemModelManager itemModels;

    public HempPressBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemModels = context.itemModelManager();
    }

    private static ItemStack part(String model) {
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of(Hempdustry.MOD_ID, model));
        return stack;
    }

    public static class State extends BlockEntityRenderState {
        final ItemRenderState platen = new ItemRenderState();
        final ItemRenderState screw = new ItemRenderState();
        /** 0 at rest under the crossbeam, 1 bottomed out on the packet. */
        float depth;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(HempPressBlockEntity press, State state, float tickDelta, Vec3d cameraPos,
                                  @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderer.super.updateRenderState(press, state, tickDelta, cameraPos, crumblingOverlay);
        itemModels.clearAndUpdate(state.platen, PLATEN, ItemDisplayContext.NONE, press.getWorld(), null, 0);
        itemModels.clearAndUpdate(state.screw, SCREW, ItemDisplayContext.NONE, press.getWorld(), null, 0);
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
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        matrices.push();
        // The item pipeline draws a model centred on the origin, so stand it in the middle of the block.
        matrices.translate(0.5F, 0.5F - state.depth * TRAVEL / 16.0F, 0.5F);
        state.platen.render(matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, 0);

        // A right-hand thread drives down turning clockwise seen from above: a negative turn about +Y.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.depth * TURNS * 360.0F));
        state.screw.render(matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();
    }
}
