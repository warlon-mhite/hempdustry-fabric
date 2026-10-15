package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.platform.Services;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Hemp Press: a screw press on a hemp-brick frame, heated from underneath.
 *
 * <p>A machine, not a vessel — it <em>transforms</em>, so it gets a screen, a heat source and a
 * recipe type ({@code hempdustry:pressing}). The logic lives in {@link HempPressBlockEntity}; this
 * is the furnace-shaped shell around it, and it borrows the Infuser's arrangement rather than the
 * Decarboxylator's because it is heated from below rather than fuelled.
 *
 * <p>A screw or lever press sits inside the community-inferred early-industrial ceiling the mod
 * works to; a hydraulic one does not, which is why it is not called that.
 *
 * <p>Deliberately <b>absent</b> from {@code FlammableBlockRegistry}: a press you set on a campfire
 * that then catches fire is a joke that stops being funny the second time. Omission is how vanilla
 * makes a block fireproof.
 */
public class HempPressBlock extends BaseEntityBlock {
    public static final MapCodec<HempPressBlock> CODEC = simpleCodec(HempPressBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /**
     * Whether the block underneath is hot. Named {@code LIT} because that is what every heated
     * vanilla block calls it — but unlike a furnace's, this one is <b>not</b> about a fire in this
     * block: it reports the neighbour, which is the same lie {@code machines.md} caught the Infuser
     * telling with its glow. The block emits no light for exactly that reason. What shows it is the
     * bed plate and the grate under it turning red — the parts the heat reaches from below — plus
     * the flame in the screen and the wisp of steam.
     */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /**
     * Whether a batch is being squeezed right now: heated, holding something the press takes, and
     * with room for the result. Puts the rosin packet on the bed, and is what the block entity
     * renderer times the platen's stroke from. Set by the server alongside {@link #LIT}.
     */
    public static final BooleanProperty PRESSING = BooleanProperty.create("pressing");

    /**
     * Bed, plate, pour lip, pillars and crossbeam, drawn facing north. The platen and the capstan are
     * left out because they move; a click in the gap lands on the plate below it. So are the lip's
     * last two pixels, which reach into the block behind it as the Infuser's spout does.
     */
    private static final double[][] FRAME = {
            {0, 0, 0, 16, 3, 16},
            {2, 3, 2, 14, 4, 14},
            {6, 3, 14, 10, 5, 16},
            {0, 3, 5, 3, 13, 11},
            {13, 3, 5, 16, 13, 11},
            {0, 13, 5, 16, 16, 11}};

    private static final Map<Direction, VoxelShape> SHAPES = HorizontalShapes.of(FRAME);

    public HempPressBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(PRESSING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, PRESSING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
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
        return new HempPressBlockEntity(pos, state);
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
        return createTickerHelper(type, ModBlockEntities.HEMP_PRESS,
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
        return world.getBlockEntity(pos) instanceof HempPressBlockEntity be ? be : null;
    }

    /** Spills whatever is in the press when it is broken. */
    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof HempPressBlockEntity be) {
                Containers.dropContents(world, pos, be);
                world.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    // ----- comparator -----

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
    }

    // ----- ambience -----

    /** Steam off a hot plate, and the occasional creak of the screw. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.9D;
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.05D) {
            world.playLocalSound(x, y, z, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON,
                    SoundSource.BLOCKS, 0.3F, 0.6F, false);
        }
        world.addParticle(ParticleTypes.SMOKE,
                x + (random.nextDouble() - 0.5D) * 0.4D, y, z + (random.nextDouble() - 0.5D) * 0.4D,
                0.0D, 0.03D, 0.0D);
    }
}
