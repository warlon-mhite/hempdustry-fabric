package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.state.BedRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla's bed renderer, drawing the hemp texture. Vanilla's own picks its texture from a
 * {@code DyeColor} out of a fixed table of sixteen, and the drawing it does is private, so this is
 * that drawing written out again — the same two model layers, the same transforms, the same render
 * layer, all read off the 1.21.11 bytecode — with one texture: {@code entity/bed/hemp.png}, which
 * the beds atlas picks up from any namespace. The bed item draws through vanilla's own
 * {@code minecraft:bed} item model pointed at that same texture, so the two always match.
 */
public class HempBedBlockEntityRenderer
        implements BlockEntityRenderer<HempBedBlockEntity, BedRenderState> {

    public static final Material TEXTURE =
            Sheets.BED_MAPPER.apply(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp"));

    private final MaterialSet sprites;
    private final Model.Simple head;
    private final Model.Simple foot;

    public HempBedBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.materials();
        this.head = new Model.Simple(context.entityModelSet().bakeLayer(ModelLayers.BED_HEAD),
                RenderTypes::entitySolid);
        this.foot = new Model.Simple(context.entityModelSet().bakeLayer(ModelLayers.BED_FOOT),
                RenderTypes::entitySolid);
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
        state.isHead = blockState.getValue(BedBlock.PART) == BedPart.HEAD;
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
        matrices.translate(0.0F, 0.5625F, 0.0F);
        matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.mulPose(Axis.ZP.rotationDegrees(180.0F + state.facing.toYRot()));
        matrices.translate(-0.5F, -0.5F, -0.5F);
        queue.submitModel(state.isHead ? head : foot, Unit.INSTANCE, matrices,
                TEXTURE.renderType(RenderTypes::entitySolid), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, sprites.get(TEXTURE), 0, state.breakProgress);
        matrices.popPose();
    }
}
