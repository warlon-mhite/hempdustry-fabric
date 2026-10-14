package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.entity.custom.BongBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
public class BongBlock extends BaseEntityBlock {
    public static final MapCodec<BongBlock> CODEC = simpleCodec(BongBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /**
     * A bowl is loaded — only the model reads it; the load itself is in the block entity with
     * everything else. Set false in the default state below: a {@code BooleanProperty} left out of
     * it defaults to <em>true</em>, the first of its two values.
     */
    public static final BooleanProperty PACKED = BooleanProperty.create("packed");

    // The chamber, the neck and lip, and the bowl on its downstem -- models/block/bong_template.json.
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(
            Shapes.or(
                    Block.box(5, 0, 5, 11, 5, 11),
                    Block.box(6, 5, 6, 10, 15, 10),
                    Block.box(11.75, 2, 6.75, 14.25, 5.75, 9.25)));

    public BongBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PACKED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PACKED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(PACKED, ctx.getItemInHand().has(ModComponents.SMOKE_CONTENTS));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BongBlockEntity(pos, state);
    }
}
