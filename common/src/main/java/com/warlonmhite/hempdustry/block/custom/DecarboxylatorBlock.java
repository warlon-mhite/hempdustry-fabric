package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Decarboxylator: a squat hemp-brick oven that gently heats buds and fan leaves until they are
 * decarboxylated and their cannabinoids will actually dissolve into fat. The first step of the
 * cannabutter chain, and the mod's first machine.
 *
 * <p>Behaves like a furnace where it should — {@link #FACING}/{@link #LIT} states, furnace hardness,
 * a comparator reading its fill level, hoppers in the top and sides and out the bottom — and unlike
 * one where the design calls for it: three trays cooking in parallel off one fire. The logic all
 * lives in {@link DecarboxylatorBlockEntity}.
 *
 * <p>Deliberately <b>absent</b> from {@code FlammableBlockRegistry}: an oven that can catch fire is
 * thematically silly, and omission is how vanilla makes a block fireproof (same treatment as the
 * hemp wood set).
 */
public class DecarboxylatorBlock extends BaseEntityBlock {
    public static final MapCodec<DecarboxylatorBlock> CODEC = simpleCodec(DecarboxylatorBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public DecarboxylatorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ----- block entity -----

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecarboxylatorBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.DECARBOXYLATOR,
                (tickWorld, pos, tickState, blockEntity) -> blockEntity.tick(tickWorld, pos, tickState));
    }

    // Opening the GUI is an empty-handed interaction, so useWithoutItem rather than useItemOn — the
    // latter would swallow the click whenever the player happened to be holding something.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            MenuProvider factory = state.getMenuProvider(world, pos);
            if (factory != null && player instanceof ServerPlayer serverPlayer) {
                // The position rides along, so the client's screen can find this block entity.
                Services.MENUS.open(serverPlayer, factory, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof DecarboxylatorBlockEntity be ? be : null;
    }

    /**
     * Tells the comparators the oven is gone. The trays, the fuel and the collected hemp have spilled
     * already: since 1.21.5 the world calls {@code BlockEntity#onBlockReplaced} first, which scatters
     * any inventory, and removes the block entity before this runs.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        Containers.updateNeighboursAfterDestroy(state, world, pos);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    // ----- comparator -----

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction side) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
    }

    // ----- ambience -----

    /** A wisp of smoke off the copper flue and a crackle while the oven is running. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.1D) {
            world.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.0F, false);
        }
        world.addParticle(ParticleTypes.SMOKE,
                x + (random.nextDouble() - 0.5D) * 0.15D, y + random.nextDouble() * 0.1D,
                z + (random.nextDouble() - 0.5D) * 0.15D, 0.0D, 0.02D, 0.0D);
    }
}
