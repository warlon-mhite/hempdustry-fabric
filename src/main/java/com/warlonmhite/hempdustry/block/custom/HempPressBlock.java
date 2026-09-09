package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
public class HempPressBlock extends BlockWithEntity {
    public static final MapCodec<HempPressBlock> CODEC = createCodec(HempPressBlock::new);

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    /**
     * Whether the block underneath is hot. Named {@code LIT} because that is what every heated
     * vanilla block calls it — but unlike a furnace's, this one is <b>not</b> about a fire in this
     * block: it reports the neighbour, which is the same lie {@code machines.md} caught the Infuser
     * telling with its glow. The block emits no light for exactly that reason; the flame in the
     * screen and the wisp of steam are where it says so.
     */
    public static final BooleanProperty LIT = Properties.LIT;

    public HempPressBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LIT, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    // ----- block entity -----

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HempPressBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.HEMP_PRESS,
                (tickWorld, pos, tickState, blockEntity) -> blockEntity.tick(tickWorld, pos, tickState));
    }

    // Opening the GUI is an empty-handed interaction, so onUse rather than onUseWithItem — the
    // latter would swallow the click whenever the player happened to be holding something.
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            NamedScreenHandlerFactory factory = state.createScreenHandlerFactory(world, pos);
            if (factory != null) {
                player.openHandledScreen(factory);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof HempPressBlockEntity be ? be : null;
    }

    /** Spills whatever is in the press when it is broken. */
    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof HempPressBlockEntity be) {
            ItemScatterer.spawn(world, pos, be);
            world.updateComparators(pos, this);
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    // ----- comparator -----

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction side) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    // ----- ambience -----

    /** Steam off a hot plate, and the occasional creak of the screw. */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.9D;
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.05D) {
            world.playSoundClient(x, y, z, SoundEvents.BLOCK_WOODEN_PRESSURE_PLATE_CLICK_ON,
                    SoundCategory.BLOCKS, 0.3F, 0.6F, false);
        }
        world.addParticleClient(ParticleTypes.SMOKE,
                x + (random.nextDouble() - 0.5D) * 0.4D, y, z + (random.nextDouble() - 0.5D) * 0.4D,
                0.0D, 0.03D, 0.0D);
    }
}
