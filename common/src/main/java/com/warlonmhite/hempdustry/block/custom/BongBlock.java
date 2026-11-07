package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.core.Registry;
import com.warlonmhite.hempdustry.block.entity.custom.BongBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
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

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /**
     * A bowl is loaded — only the model reads it; the load itself is in the block entity with
     * everything else. Set false in the default state below: a {@code BooleanProperty} left out of
     * it defaults to <em>true</em>, the first of its two values.
     */
    public static final BooleanProperty PACKED = BooleanProperty.create("packed");

    // The chamber, the neck and lip, and the bowl on its downstem -- models/block/bong_template.json.
    private static final Map<Direction, VoxelShape> SHAPES = HorizontalShapes.of(new double[][]{
            {5, 0, 5, 11, 5, 11},
            {6, 5, 6, 10, 15, 10},
            {11.75, 2, 6.75, 14.25, 5.75, 9.25}});

    public BongBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PACKED, false));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PACKED);
    }

    /**
     * The device item's key, not a {@code block.*} one: a placed Red Bong is named the way the bong
     * in your hand is, with no second line in every locale to say the same thing.
     */
    @Override
    public String getDescriptionId() {
        return Util.makeDescriptionId("item", Registry.BLOCK.getKey(this));
    }

    /**
     * Drawn from its block model. {@code BaseEntityBlock} answers {@code INVISIBLE} (up to 1.21.1),
     * for block entities that draw themselves; this one only holds the stack, so without this the
     * standing bong is an outline with nothing in it.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(PACKED, ModComponents.SMOKE_CONTENTS.has(ctx.getItemInHand()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BongBlockEntity(pos, state);
    }

    /**
     * The device comes back as it went down: the loot table drops the bong, and the block entity hands
     * the dropped item the tag the placed one carried.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BongBlockEntity bong) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    bong.giveTo(drop);
                }
            }
        }
        return drops;
    }
}
