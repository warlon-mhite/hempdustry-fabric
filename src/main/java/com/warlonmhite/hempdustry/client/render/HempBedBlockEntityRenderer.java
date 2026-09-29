package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.LightmapCoordinatesRetriever;
import net.minecraft.client.render.block.entity.state.BedBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
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
        implements BlockEntityRenderer<HempBedBlockEntity, BedBlockEntityRenderState> {

    public static final SpriteIdentifier TEXTURE =
            TexturedRenderLayers.BED_SPRITE_MAPPER.map(Identifier.of(Hempdustry.MOD_ID, "hemp"));

    private final SpriteHolder sprites;
    private final Model.SinglePartModel head;
    private final Model.SinglePartModel foot;

    public HempBedBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.sprites = context.spriteHolder();
        this.head = new Model.SinglePartModel(context.loadedEntityModels().getModelPart(EntityModelLayers.BED_HEAD),
                RenderLayers::entitySolid);
        this.foot = new Model.SinglePartModel(context.loadedEntityModels().getModelPart(EntityModelLayers.BED_FOOT),
                RenderLayers::entitySolid);
    }

    @Override
    public BedBlockEntityRenderState createRenderState() {
        return new BedBlockEntityRenderState();
    }

    @Override
    public void updateRenderState(HempBedBlockEntity bed, BedBlockEntityRenderState state, float tickDelta,
                                  Vec3d cameraPos, @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderer.super.updateRenderState(bed, state, tickDelta, cameraPos, crumblingOverlay);
        BlockState blockState = bed.getCachedState();
        state.facing = blockState.get(BedBlock.FACING);
        state.headPart = blockState.get(BedBlock.PART) == BedPart.HEAD;
        if (bed.getWorld() != null) {
            // Both halves take the brighter half's light, so a bed half in shadow is not two-tone.
            state.lightmapCoordinates = DoubleBlockProperties.toPropertySource(ModBlockEntities.HEMP_BED,
                            BedBlock::getBedPart, BedBlock::getOppositePartDirection, BedBlock.FACING,
                            blockState, bed.getWorld(), bed.getPos(), (world, pos) -> false)
                    .apply(new LightmapCoordinatesRetriever<>())
                    .get(state.lightmapCoordinates);
        }
    }

    @Override
    public void render(BedBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                       CameraRenderState camera) {
        matrices.push();
        matrices.translate(0.0F, 0.5625F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F + state.facing.getPositiveHorizontalDegrees()));
        matrices.translate(-0.5F, -0.5F, -0.5F);
        queue.submitModel(state.headPart ? head : foot, Unit.INSTANCE, matrices,
                TEXTURE.getRenderLayer(RenderLayers::entitySolid), state.lightmapCoordinates,
                OverlayTexture.DEFAULT_UV, -1, sprites.getSprite(TEXTURE), 0, state.crumblingOverlay);
        matrices.pop();
    }
}
