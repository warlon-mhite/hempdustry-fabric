package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.LightmapCoordinatesRetriever;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

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

    public static final SpriteIdentifier TEXTURE = new SpriteIdentifier(TexturedRenderLayers.BEDS_ATLAS_TEXTURE,
            Identifier.of(Hempdustry.MOD_ID, "entity/bed/hemp"));

    private final ModelPart head;
    private final ModelPart foot;

    public HempBedBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.head = context.getLayerModelPart(EntityModelLayers.BED_HEAD);
        this.foot = context.getLayerModelPart(EntityModelLayers.BED_FOOT);
    }

    @Override
    public void render(HempBedBlockEntity bed, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = bed.getWorld();
        if (world == null) {
            renderPart(matrices, vertexConsumers, head, Direction.SOUTH, light, overlay, false);
            renderPart(matrices, vertexConsumers, foot, Direction.SOUTH, light, overlay, true);
            return;
        }
        BlockState state = bed.getCachedState();
        // Both halves take the brighter half's light, so a bed half in shadow is not two-tone.
        int lit = DoubleBlockProperties.toPropertySource(ModBlockEntities.HEMP_BED, BedBlock::getBedPart,
                        BedBlock::getOppositePartDirection, BedBlock.FACING, state, world, bed.getPos(),
                        (w, pos) -> false)
                .apply(new LightmapCoordinatesRetriever<>())
                .get(light);
        renderPart(matrices, vertexConsumers, state.get(BedBlock.PART) == BedPart.HEAD ? head : foot,
                state.get(BedBlock.FACING), lit, overlay, false);
    }

    private static void renderPart(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ModelPart part,
                                   Direction facing, int light, int overlay, boolean foot) {
        matrices.push();
        matrices.translate(0.0F, 0.5625F, foot ? -1.0F : 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F + facing.asRotation()));
        matrices.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer buffer = TEXTURE.getVertexConsumer(vertexConsumers, RenderLayer::getEntitySolid);
        part.render(matrices, buffer, light, overlay);
        matrices.pop();
    }
}
