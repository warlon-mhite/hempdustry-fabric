package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
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
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
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
 * hemp wood set — see CLAUDE.md §3).
 */
public class DecarboxylatorBlock extends BlockWithEntity {
    public static final MapCodec<DecarboxylatorBlock> CODEC = createCodec(DecarboxylatorBlock::new);

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;

    public DecarboxylatorBlock(Settings settings) {
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
        return new DecarboxylatorBlockEntity(pos, state);
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
        return validateTicker(type, ModBlockEntities.DECARBOXYLATOR,
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
        return world.getBlockEntity(pos) instanceof DecarboxylatorBlockEntity be ? be : null;
    }

    /** Spills the trays, the fuel and the collected hemp when the oven is broken. */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof DecarboxylatorBlockEntity be) {
                ItemScatterer.spawn(world, pos, be);
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    // ----- comparator -----

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    // ----- ambience -----

    /** A wisp of smoke off the copper flue and a crackle while the oven is running. */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.1D) {
            world.playSound(x, y, z, net.minecraft.sound.SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.6F, 1.0F, false);
        }
        world.addParticle(ParticleTypes.SMOKE,
                x + (random.nextDouble() - 0.5D) * 0.15D, y + random.nextDouble() * 0.1D,
                z + (random.nextDouble() - 0.5D) * 0.15D, 0.0D, 0.02D, 0.0D);
    }
}
