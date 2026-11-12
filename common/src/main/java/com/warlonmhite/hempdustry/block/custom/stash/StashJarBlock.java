package com.warlonmhite.hempdustry.block.custom.stash;

import com.warlonmhite.hempdustry.block.entity.custom.StashJarBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.component.StashContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A Stash Jar set down: it is placed upright on a top face that holds a lantern or a rail (a hopper's
 * rim included), then needs nothing more, as a shulker box needs nothing: it stays where it was put
 * when the block below goes, so a fire or an enderman under it never drops it into harm's way. It drops
 * itself with everything inside, as a shulker box does (the loot table copies the contents back onto
 * the item). No menu: a storable item clicked on it goes in, as many as fit; an empty hand takes out
 * the last stack put in; anything else in hand is that item's own use.
 *
 * <p>One block per tier, the capacity its own, so the item a jar drops is the jar it was.
 *
 * <p>Set down in water it keeps the water, as a lantern does, and leaves it behind when broken.
 */
public class StashJarBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    // The glass body and the lid, as models/block/stash_jar_*.json draw them.
    private static final VoxelShape SHAPE = Shapes.or(Block.column(8.0, 0.0, 10.0), Block.column(6.0, 10.0, 12.0));

    private final int capacity;

    public StashJarBlock(Properties settings, int capacity) {
        super(settings);
        this.capacity = capacity;
        registerDefaultState(defaultBlockState().setValue(StashFill.PROPERTY, StashFill.EMPTY).setValue(BlockStateProperties.WATERLOGGED, false));
    }

    /** Items this jar holds, every item one, whatever its stack size. */
    public int capacity() {
        return capacity;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(StashFill.PROPERTY, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StashJarBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Upright whatever face was clicked, or not at all where the face below would hold neither a
     * lantern ({@code canSupportCenter}) nor a rail ({@code SupportType.RIGID}, which a hopper's rim
     * gives). Asked only here: once placed, a jar stands on its own.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        StashContents contents = context.getItemInHand().getOrDefault(ModComponents.STASH_CONTENTS, StashContents.EMPTY);
        BlockState state = defaultBlockState().setValue(StashFill.PROPERTY, StashFill.of(contents.count(), capacity))
                .setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
        return canStandOn(context.getLevel(), context.getClickedPos().below()) ? state : null;
    }

    // Waterlogging, as vanilla's lantern does it.
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /** Whether a jar may be placed on the block at {@code below}: a lantern's floor or a rail's. */
    public static boolean canStandOn(LevelReader level, BlockPos below) {
        return Block.canSupportCenter(level, below, Direction.UP)
                || level.getBlockState(below).isFaceSturdy(level, below, Direction.UP, SupportType.RIGID);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!StashContents.storable(stack) || !(level.getBlockEntity(pos) instanceof StashJarBlockEntity jar)) {
            // Not ours to take: the empty-hand half below declines too while anything is held, so the
            // item's own use goes ahead.
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Creative takes no items out of the hand, as with any block it fills.
        ItemStack moving = player.hasInfiniteMaterials() ? stack.copy() : stack;
        if (jar.insert(moving) > 0) {
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F,
                    0.8F + level.getRandom().nextFloat() * 0.4F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        } else {
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        // Asked after useItemOn declined, so a held item that is not storable lands here too. Only an
        // empty hand takes a stack out.
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof StashJarBlockEntity jar)
                || jar.shownCount() == 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack out = jar.removeLast();
        player.setItemInHand(InteractionHand.MAIN_HAND, out);
        level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8F,
                0.8F + level.getRandom().nextFloat() * 0.4F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }

    /**
     * A creative player breaking a jar that holds something gets it back, as with a shulker box
     * ({@code ShulkerBoxBlock#playerWillDestroy}); an empty one is gone, as an empty box is.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel && player.preventsBlockDrops()
                && level.getBlockEntity(pos) instanceof StashJarBlockEntity jar && !jar.contents().isEmpty()) {
            ItemStack dropped = new ItemStack(this);
            dropped.applyComponents(jar.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, dropped);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** The shulker box's reading ({@code getRedstoneSignalFromContainer}): 0 empty, 1 with one item, 15 full. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof StashJarBlockEntity jar
                ? signal(jar.contents().count(), jar.capacity()) : 0;
    }

    /** {@code count} items of {@code capacity} as a comparator reads them. */
    public static int signal(int count, int capacity) {
        return Mth.lerpDiscrete(capacity <= 0 ? 0.0F : Math.min(1.0F, (float) count / capacity), 0, 15);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
