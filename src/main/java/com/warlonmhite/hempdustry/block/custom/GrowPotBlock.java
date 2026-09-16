package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * A planter for hemp, either strain. It stands in for farmland: always watered, never trampled, and
 * a plant in it never competes with its neighbours for roots — vanilla halves a crop's moisture when
 * the same crop stands beside it, and a pot's plant has soil of its own.
 *
 * <p><b>It is only as good as its soil.</b> {@link #FERTILITY} runs 0–{@value #MAX_FERTILITY}; a
 * fertile pot grows its plant {@value #FERTILE_SPEED}× faster than the best field, and a spent one
 * like dry farmland. Every plant that ripens in it spends one, whatever made it ripen, and bone
 * meal puts one back — hemp is a nitrogen-hungry plant of manured ground, so the pot runs on the
 * composter. A full pot refuses bone meal rather than eating it, the way a full composter refuses.
 *
 * <p>The fertility is in the blockstate, so the pot keeps it when picked up (the loot table copies
 * it, as a beehive keeps its honey) and a freshly crafted pot starts empty.
 */
public class GrowPotBlock extends Block implements GrowMedium {
    public static final MapCodec<GrowPotBlock> CODEC = createCodec(GrowPotBlock::new);

    public static final int MAX_FERTILITY = 3;
    public static final IntProperty FERTILITY = IntProperty.of("fertility", 0, MAX_FERTILITY);

    /** Vanilla's best: a lone plant on moist farmland ringed by moist farmland. */
    public static final float FERTILE_MOISTURE = 10.0F;
    /** Dry farmland ringed by dry farmland — the soil is spent, not gone. */
    public static final float SPENT_MOISTURE = 4.0F;
    public static final float FERTILE_SPEED = 1.5F;

    /** A body inset a pixel under a full-width lip, as the model draws it. */
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 13.0D, 15.0D),
            Block.createCuboidShape(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D));

    public GrowPotBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FERTILITY, 0));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    public static boolean isPot(BlockState state) {
        return state.getBlock() instanceof GrowPotBlock;
    }

    public static boolean isFertile(BlockState state) {
        return isPot(state) && state.get(FERTILITY) > 0;
    }

    @Override
    public float moisture(BlockState state) {
        return isFertile(state) ? FERTILE_MOISTURE : SPENT_MOISTURE;
    }

    @Override
    public float speed(BlockState state) {
        return isFertile(state) ? FERTILE_SPEED : 1.0F;
    }

    @Override
    public void spend(World world, BlockPos pos, BlockState state) {
        if (isFertile(state)) {
            world.setBlockState(pos, state.with(FERTILITY, state.get(FERTILITY) - 1), Block.NOTIFY_LISTENERS);
        }
    }

    /** Any fertiliser in {@code #c:fertilizers} feeds the soil; anything else falls through. */
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        ActionResult fed = GrowMedium.tryFeed(stack, state, world, pos, player);
        return fed != null ? fed : super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    // ----- bone meal feeds the soil -----

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return state.get(FERTILITY) < MAX_FERTILITY;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(FERTILITY, state.get(FERTILITY) + 1), Block.NOTIFY_LISTENERS);
    }

    /**
     * Breaking the pot under a plant harvests the plant first, while the pot is still there.
     * Otherwise the plant pops off after the pot has gone, its loot table no longer sees a pot under
     * it, and breaking the pot becomes the way round the root-bound stem.
     */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockPos above = pos.up();
        if (!world.isClient() && world.getBlockState(above).isIn(ModTags.Blocks.HEMP_CROPS)) {
            world.breakBlock(above, !player.isCreative(), player);
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FERTILITY);
    }
}
