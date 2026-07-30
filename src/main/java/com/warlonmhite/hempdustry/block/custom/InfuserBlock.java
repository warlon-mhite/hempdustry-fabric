package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Infuser — a hempcrete tub built around a cauldron, which simmers decarboxylated hemp into a
 * bucket of milk. All the interesting behaviour lives in {@link InfuserBlockEntity}; this class is
 * the shell, the {@link #HEATED} state, and the ambience that teaches the mechanic without a word
 * of text.
 *
 * <p><b>{@link #HEATED} is not a fuel state, it is a report about the neighbourhood.</b> The block
 * entity sets it from whatever sits underneath, so the visual difference between a cold tub and a
 * bubbling one <em>is</em> the tutorial for heat-from-below: put it on a campfire and it visibly
 * comes alive. That is the "you are not wearing a HUD" principle doing real work — no tooltip
 * explains this anywhere, and none should have to.
 *
 * <p>Deliberately absent from {@code FlammableBlockRegistry}, like the rest of the mod's machinery.
 */
public class InfuserBlock extends BlockWithEntity {
    public static final MapCodec<InfuserBlock> CODEC = createCodec(InfuserBlock::new);

    /** Set by the block entity from the block below — never by the player, and never persisted alone. */
    public static final BooleanProperty HEATED = BooleanProperty.of("heated");

    public InfuserBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(HEATED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HEATED);
    }

    // ----- block entity -----

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InfuserBlockEntity(pos, state);
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
        return validateTicker(type, ModBlockEntities.INFUSER,
                (tickWorld, pos, tickState, blockEntity) -> blockEntity.tick(tickWorld, pos, tickState));
    }

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
        return world.getBlockEntity(pos) instanceof InfuserBlockEntity be ? be : null;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof InfuserBlockEntity be) {
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

    /**
     * Reports <b>batch progress</b> rather than how full the container is — see
     * {@link InfuserBlockEntity#getComparatorOutput()} for why fill level would tell a player
     * nothing useful here.
     */
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof InfuserBlockEntity infuser
                ? infuser.getComparatorOutput()
                : 0;
    }

    // ----- ambience -----

    /**
     * Steam and a soft bubbling while it simmers. Note the glow is deliberately <em>not</em> emitted
     * by this block: the light comes from whatever is heating it, so a lit Infuser looks lit
     * because of the campfire under it. Self-illuminating would quietly undercut the whole
     * heat-from-below idea.
     */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(HEATED)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.9D;
        double z = pos.getZ() + 0.5D;
        if (random.nextDouble() < 0.05D) {
            world.playSound(x, y, z, SoundEvents.BLOCK_BREWING_STAND_BREW,
                    SoundCategory.BLOCKS, 0.35F, 0.8F + random.nextFloat() * 0.4F, false);
        }
        for (int i = 0; i < 2; i++) {
            world.addParticle(ParticleTypes.BUBBLE_POP,
                    x + (random.nextDouble() - 0.5D) * 0.5D, y,
                    z + (random.nextDouble() - 0.5D) * 0.5D, 0.0D, 0.0D, 0.0D);
        }
        world.addParticle(ParticleTypes.CLOUD,
                x + (random.nextDouble() - 0.5D) * 0.3D, y + 0.15D,
                z + (random.nextDouble() - 0.5D) * 0.3D, 0.0D, 0.03D, 0.0D);
    }
}
