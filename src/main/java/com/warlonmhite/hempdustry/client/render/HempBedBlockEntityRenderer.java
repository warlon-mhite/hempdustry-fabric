package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Vanilla's bed renderer, drawing the hemp texture. Vanilla's own picks its texture from a
 * {@code DyeColor} out of a fixed table of sixteen, and the drawing it does is private, so this is
 * that drawing written out again — the same two model layers, the same transforms, the same render
 * layer, all read off the 1.21.1 bytecode — with one texture: {@code entity/bed/hemp.png}, which the
 * beds atlas picks up from any namespace.
 *
 * <p>With no world — the block entity the bed <em>item</em> is drawn from — it draws both halves
 * facing south, as vanilla's does for its own bed items, so the one in your hand matches the one on
 * the floor.
 */
public class HempBedBlockEntityRenderer implements BlockEntityRenderer<HempBedBlockEntity> {

    public static final Material TEXTURE = new Material(Sheets.BED_SHEET,
            ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "entity/bed/hemp"));

    private final ModelPart head;
    private final ModelPart foot;

    public HempBedBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.head = context.bakeLayer(ModelLayers.BED_HEAD);
        this.foot = context.bakeLayer(ModelLayers.BED_FOOT);
    }

    @Override
    public void render(HempBedBlockEntity bed, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = bed.getLevel();
        if (world == null) {
            renderPart(matrices, vertexConsumers, head, Direction.SOUTH, light, overlay, false);
            renderPart(matrices, vertexConsumers, foot, Direction.SOUTH, light, overlay, true);
            return;
        }
        BlockState state = bed.getBlockState();
        // Both halves take the brighter half's light, so a bed half in shadow is not two-tone.
        int lit = DoubleBlockCombiner.combineWithNeigbour(ModBlockEntities.HEMP_BED, BedBlock::getBlockType,
                        BedBlock::getConnectedDirection, BedBlock.FACING, state, world, bed.getBlockPos(),
                        (w, pos) -> false)
                .apply(new BrightnessCombiner<>())
                .get(light);
        renderPart(matrices, vertexConsumers, state.getValue(BedBlock.PART) == BedPart.HEAD ? head : foot,
                state.getValue(BedBlock.FACING), lit, overlay, false);
    }

    private static void renderPart(PoseStack matrices, MultiBufferSource vertexConsumers, ModelPart part,
                                   Direction facing, int light, int overlay, boolean foot) {
        matrices.pushPose();
        matrices.translate(0.0F, 0.5625F, foot ? -1.0F : 0.0F);
        matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.mulPose(Axis.ZP.rotationDegrees(180.0F + facing.toYRot()));
        matrices.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer buffer = TEXTURE.buffer(vertexConsumers, RenderType::entitySolid);
        part.render(matrices, buffer, light, overlay);
        matrices.popPose();
    }
}
