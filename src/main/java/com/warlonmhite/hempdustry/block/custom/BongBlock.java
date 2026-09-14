package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.entity.custom.BongBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A bong standing on something — empty, or packed and set down by sneaking to be picked up and
 * smoked later. Pure decoration, the way a flower pot or a candle is: it
 * does nothing when clicked, breaks instantly and always drops itself — a Silk-Touch rule like
 * glass's would shatter an enchanted device for the crime of being put down.
 *
 * <p>The item is placed by {@link com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem}, not by
 * a {@code BlockItem}, because one class is every smoking device. One block per glass, like beds
 * and candles; every one shares {@link BongBlockEntity}, whose only job is to hold the stack's
 * components until the block is broken.
 */
public class BongBlock extends BlockWithEntity {
    public static final MapCodec<BongBlock> CODEC = createCodec(BongBlock::new);

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    /**
     * A bowl is loaded — only the model reads it; the load itself is in the block entity with
     * everything else. Set false in the default state below: a {@code BooleanProperty} left out of
     * it defaults to <em>true</em> (CLAUDE.md §5).
     */
    public static final BooleanProperty PACKED = BooleanProperty.of("packed");

    // The chamber, the neck and lip, and the bowl on its downstem -- models/block/bong_template.json.
    private static final Map<Direction, VoxelShape> SHAPES = VoxelShapes.createHorizontalFacingShapeMap(
            VoxelShapes.union(
                    Block.createCuboidShape(5, 0, 5, 11, 5, 11),
                    Block.createCuboidShape(6, 5, 6, 10, 15, 10),
                    Block.createCuboidShape(11.75, 2, 6.75, 14.25, 5.75, 9.25)));

    public BongBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(PACKED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PACKED);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(PACKED, ctx.getStack().contains(ModComponents.SMOKE_CONTENTS));
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BongBlockEntity(pos, state);
    }
}
