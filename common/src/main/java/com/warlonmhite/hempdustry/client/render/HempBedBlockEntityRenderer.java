package com.warlonmhite.hempdustry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
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
 * that drawing written out again — the same parts, the same transforms, the same render layer, all
 * read off the 1.16.5 bytecode — with one texture: {@code entity/bed/hemp.png}, stitched into the
 * beds atlas by {@code HempdustryClient} (1.16.5's atlas takes only the sprites it is told of).
 *
 * <p>On 1.16.5 a bed's model is built here rather than baked from a layer: the head and foot boards
 * and the four legs, each drawn only on the half it belongs to, as vanilla's {@code BedRenderer} does.
 *
 * <p>With no world — the block entity the bed <em>item</em> is drawn from — it draws both halves
 * facing south, as vanilla's does for its own bed items, so the one in your hand matches the one on
 * the floor.
 */
public class HempBedBlockEntityRenderer extends BlockEntityRenderer<HempBedBlockEntity> {

    public static final Material TEXTURE = new Material(Sheets.BED_SHEET,
            new ResourceLocation(Hempdustry.MOD_ID, "entity/bed/hemp"));

    /** The block entity the bed item is drawn from: no world, so {@link #render} draws both halves. */
    private static HempBedBlockEntity itemBed;

    private final ModelPart headPiece;
    private final ModelPart footPiece;
    private final ModelPart[] legs = new ModelPart[4];

    /**
     * Draws the bed item. The loader's item hook calls this, since the item model hands a bed to
     * vanilla's built-in item renderer, which draws any {@code BedBlock} as the vanilla bed of its
     * {@code DyeColor} -- a white one, for this bed.
     */
    public static void renderItem(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        if (itemBed == null) {
            itemBed = ModBlockEntities.HEMP_BED.create();
        }
        BlockEntityRenderDispatcher.instance.renderItem(itemBed, matrices, vertexConsumers, light, overlay);
    }

    public HempBedBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
        this.headPiece = new ModelPart(64, 64, 0, 0);
        this.headPiece.addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F, 0.0F);
        this.footPiece = new ModelPart(64, 64, 0, 22);
        this.footPiece.addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F, 0.0F);
        this.legs[0] = new ModelPart(64, 64, 50, 0);
        this.legs[1] = new ModelPart(64, 64, 50, 6);
        this.legs[2] = new ModelPart(64, 64, 50, 12);
        this.legs[3] = new ModelPart(64, 64, 50, 18);
        this.legs[0].addBox(0.0F, 6.0F, -16.0F, 3.0F, 3.0F, 3.0F);
        this.legs[1].addBox(0.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F);
        this.legs[2].addBox(-16.0F, 6.0F, -16.0F, 3.0F, 3.0F, 3.0F);
        this.legs[3].addBox(-16.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F);
        for (ModelPart leg : this.legs) {
            leg.xRot = (float) (Math.PI / 2);
        }
        this.legs[0].zRot = 0.0F;
        this.legs[1].zRot = (float) (Math.PI / 2);
        this.legs[2].zRot = (float) (Math.PI * 3.0 / 2.0);
        this.legs[3].zRot = (float) Math.PI;
    }

    @Override
    public void render(HempBedBlockEntity bed, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = bed.getLevel();
        if (world == null) {
            renderPiece(matrices, vertexConsumers, true, Direction.SOUTH, light, overlay, false);
            renderPiece(matrices, vertexConsumers, false, Direction.SOUTH, light, overlay, true);
            return;
        }
        BlockState state = bed.getBlockState();
        // Both halves take the brighter half's light, so a bed half in shadow is not two-tone.
        int lit = DoubleBlockCombiner.combineWithNeigbour(ModBlockEntities.HEMP_BED, BedBlock::getBlockType,
                        BedBlock::getConnectedDirection, BedBlock.FACING, state, world, bed.getBlockPos(),
                        (w, pos) -> false)
                .apply(new BrightnessCombiner<>())
                .get(light);
        renderPiece(matrices, vertexConsumers, state.getValue(BedBlock.PART) == BedPart.HEAD,
                state.getValue(BedBlock.FACING), lit, overlay, false);
    }

    private void renderPiece(PoseStack matrices, MultiBufferSource vertexConsumers, boolean head,
                             Direction facing, int light, int overlay, boolean foot) {
        this.headPiece.visible = head;
        this.footPiece.visible = !head;
        this.legs[0].visible = !head;
        this.legs[1].visible = head;
        this.legs[2].visible = !head;
        this.legs[3].visible = head;
        matrices.pushPose();
        matrices.translate(0.0D, 0.5625D, foot ? -1.0D : 0.0D);
        matrices.mulPose(Vector3f.XP.rotationDegrees(90.0F));
        matrices.translate(0.5D, 0.5D, 0.5D);
        matrices.mulPose(Vector3f.ZP.rotationDegrees(180.0F + facing.toYRot()));
        matrices.translate(-0.5D, -0.5D, -0.5D);
        VertexConsumer buffer = TEXTURE.buffer(vertexConsumers, RenderType::entitySolid);
        this.headPiece.render(matrices, buffer, light, overlay);
        this.footPiece.render(matrices, buffer, light, overlay);
        for (ModelPart leg : this.legs) {
            leg.render(matrices, buffer, light, overlay);
        }
        matrices.popPose();
    }
}
