package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

/**
 * The Grow Lamp: an LED panel hung from the ceiling by two copper chains, purple diodes underneath.
 *
 * <p>It is vanilla's redstone lamp in everything that matters — lit by a signal the same way, off
 * four ticks after the signal goes — so a redstone torch <em>on</em> it does nothing, exactly as on a
 * redstone lamp. What makes it a grow light is {@code #hempdustry:grow_lamps}, which
 * {@link GrowLight} reads. What this class adds is hanging: it needs a ceiling, as a hanging lantern
 * does, and drops when the ceiling goes.
 *
 * <p>It used to be a full copper cube whose only lit face was the underside, so a powered lamp looked
 * identical to an unpowered one from anywhere but directly beneath — and in daylight, where block
 * light is invisible, that read as "powered, gives no light". The panel shows its diodes and a lit
 * edge strip from every angle.
 */
public class GrowLampBlock extends RedstoneLampBlock {
    public static final MapCodec<RedstoneLampBlock> CODEC = createCodec(GrowLampBlock::new);

    /** The panel, and the two chains it hangs from, as the model draws them. */
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1.0D, 4.0D, 1.0D, 15.0D, 7.0D, 15.0D),
            Block.createCuboidShape(2.5D, 7.0D, 6.5D, 5.5D, 16.0D, 9.5D),
            Block.createCuboidShape(10.5D, 7.0D, 6.5D, 13.5D, 16.0D, 9.5D));

    public GrowLampBlock(Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<RedstoneLampBlock> getCodec() {
        return CODEC;
    }

    /** Vanilla's placement (lit if already powered), refused where there is no ceiling to hang from. */
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state != null && state.canPlaceAt(ctx.getWorld(), ctx.getBlockPos()) ? state : null;
    }

    /** The same test a hanging lantern makes: something under the block above to hang the chains from. */
    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return Block.sideCoversSmallSquare(world, pos.up(), Direction.DOWN);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        return direction == Direction.UP && !state.canPlaceAt(world, pos)
                ? Blocks.AIR.getDefaultState()
                : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    /**
     * Minecraft light has no colour, and a shader pack cannot be told a modded block's colour, so a
     * lit lamp shows its UV glow the one way every renderer draws: violet dust hanging in the air
     * under the panel. Client-side and cosmetic, at vanilla's own display-tick rate — the same hook
     * redstone ore sparkles on.
     */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(LIT)) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            world.addParticleClient(UV_GLOW,
                    pos.getX() + 0.1 + random.nextDouble() * 0.8,
                    pos.getY() + 0.2 - random.nextDouble() * 1.2,
                    pos.getZ() + 0.1 + random.nextDouble() * 0.8,
                    0.0, -0.02, 0.0);
        }
    }

    /** The diodes' violet, and a little smaller than redstone's dust. */
    private static final DustParticleEffect UV_GLOW = new DustParticleEffect(0x8A3CFF, 0.7F);
}
