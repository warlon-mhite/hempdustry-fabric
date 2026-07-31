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
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
 * The Infuser — a hempcrete tub built around a cauldron, which simmers decarboxylated hemp into a
 * bucket of milk. All the interesting behaviour lives in {@link InfuserBlockEntity}; this class is
 * the shell, its three states, and the ambience that teaches the mechanic without a word of text.
 *
 * <p><b>The spout ({@link #FACING}) exists because heat-from-below claimed the extraction face.</b>
 * A hopper pulls from the inventory above it, through that inventory's <em>down</em> face — which is
 * exactly the block the Infuser reads its heat from. Campfire below means no hopper; hopper below
 * means no heat. So output automation is impossible by the normal route, and the tub pushes a
 * finished batch out of a spout instead. Making that a visible spout rather than an invisible
 * "pushes to any adjacent container" rule is the point: nothing else in the game would tell you.
 *
 * <p><b>Two states, because the block has two different things to say</b>, and conflating them was
 * the original mistake: a single {@code heated} property meant a tub sitting on a campfire with
 * nothing in it looked and sounded like a working one.
 * <ul>
 *   <li>{@link #FILLED} — there is milk in the tub, either waiting in the slot or already committed
 *       to a batch. This is what the <b>texture</b> keys off, and it is deliberately independent of
 *       heat: what makes a tub look full is liquid being in it.</li>
 *   <li>{@link #INFUSING} — a batch is actually simmering. This is what the <b>bubbling and steam</b>
 *       key off, so the ambience only ever means "something is happening in here", and stops when
 *       the batch finishes.</li>
 * </ul>
 * Between them they still teach heat-from-below without a word of text — put a filled tub on a
 * campfire and it starts working in front of you — which is the "you are not wearing a HUD"
 * principle doing real work.
 *
 * <p>Deliberately absent from {@code FlammableBlockRegistry}, like the rest of the mod's machinery.
 */
public class InfuserBlock extends BlockWithEntity {
    public static final MapCodec<InfuserBlock> CODEC = createCodec(InfuserBlock::new);

    /**
     * Which way the spout points. <b>This is the block teaching its own automation rule.</b> The
     * Infuser cannot be drained by a hopper underneath — that block is where its heat comes from —
     * so instead it pushes a finished batch out of the spout into whatever inventory is against that
     * face. A hidden "pushes to some adjacent container" rule would be unguessable; a spout you can
     * see is not. See {@link InfuserBlockEntity#pushOutput}.
     */
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    /** Milk is in the tub. Drives the model. Set by the block entity, never by the player. */
    public static final BooleanProperty FILLED = BooleanProperty.of("filled");
    /** A batch is simmering right now. Drives the particles. Set by the block entity. */
    public static final BooleanProperty INFUSING = BooleanProperty.of("infusing");

    public InfuserBlock(Settings settings) {
        super(settings);
        // The booleans are spelled out: an omitted BooleanProperty defaults to *true* (its value set
        // is ImmutableSet.of(true, false) and the default state is states.get(0)).
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(FILLED, false)
                .with(INFUSING, false));
    }

    /** Spout away from the player, so you place the tub facing the chest you are looking at. */
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

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILLED, INFUSING);
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
                // Order matters. The preview is not a real item, so it goes in the bin first —
                // otherwise breaking a ready tub would drop the cannabutter *and* refund the hemp
                // below. What a spilled batch gives back is its ingredients.
                be.discardPreview();
                ItemScatterer.spawn(world, pos, be);
                // Hemp already drawn into a running batch has left the slots, so it has to be
                // spilled separately or breaking a simmering tub would destroy it.
                ItemScatterer.spawn(world, pos, be.getBatchItems());
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
     * Steam and a soft bubbling <b>only while a batch is actually simmering</b> — not merely because
     * something hot is underneath. A heated but empty tub is silent and still, which is the honest
     * signal: the noise means work is being done.
     *
     * <p>Note the glow is deliberately <em>not</em> emitted by this block: the light comes from
     * whatever is heating it, so a lit Infuser looks lit because of the campfire under it.
     * Self-illuminating would quietly undercut the whole heat-from-below idea.
     */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(INFUSING)) {
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
