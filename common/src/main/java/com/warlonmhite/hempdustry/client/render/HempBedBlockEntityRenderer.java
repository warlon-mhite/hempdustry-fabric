package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.state.BedRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla's bed renderer, drawing the hemp texture. Vanilla's own picks its texture from the bed's
 * {@code DyeColor}, but the piece it draws is open to anyone ({@code BedRenderer#submitPiece} takes the
 * sprite), so this keeps vanilla's renderer and hands it {@code entity/bed/hemp.png}, which the beds
 * atlas picks up from any namespace. The bed item draws through vanilla's own {@code minecraft:bed}
 * special model pointed at that same texture, so the two always match.
 */
public class HempBedBlockEntityRenderer
        implements BlockEntityRenderer<HempBedBlockEntity, BedRenderState> {

    public static final SpriteId TEXTURE =
            Sheets.BED_MAPPER.apply(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp"));

    private final BedRenderer vanilla;

    public HempBedBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.vanilla = new BedRenderer(context);
    }

    @Override
    public BedRenderState createRenderState() {
        return new BedRenderState();
    }

    @Override
    public void extractRenderState(HempBedBlockEntity bed, BedRenderState state, float tickDelta,
                                  Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(bed, state, tickDelta, cameraPos, crumblingOverlay);
        BlockState blockState = bed.getBlockState();
        state.facing = blockState.getValue(BedBlock.FACING);
        state.part = blockState.getValue(BedBlock.PART);
        if (bed.getLevel() != null) {
            // Both halves take the brighter half's light, so a bed half in shadow is not two-tone.
            state.lightCoords = DoubleBlockCombiner.combineWithNeigbour(ModBlockEntities.HEMP_BED,
                            BedBlock::getBlockType, BedBlock::getConnectedDirection, BedBlock.FACING,
                            blockState, bed.getLevel(), bed.getBlockPos(), (world, pos) -> false)
                    .apply(new BrightnessCombiner<>())
                    .get(state.lightCoords);
        }
    }

    @Override
    public void submit(BedRenderState state, PoseStack matrices, SubmitNodeCollector queue,
                       CameraRenderState camera) {
        matrices.pushPose();
        matrices.mulPose(BedRenderer.modelTransform(state.facing));
        vanilla.submitPiece(state.part, TEXTURE, matrices, queue, state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.breakProgress, 0);
        matrices.popPose();
    }
}
