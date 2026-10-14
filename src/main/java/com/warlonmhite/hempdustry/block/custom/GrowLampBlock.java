package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Grow Lamp: an LED panel hung from the ceiling by two copper chains, a violet redstone lamp's
 * face underneath.
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
    public static final MapCodec<RedstoneLampBlock> CODEC = simpleCodec(GrowLampBlock::new);

    /** The panel, and the two chains it hangs from, as the model draws them. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0D, 4.0D, 1.0D, 15.0D, 7.0D, 15.0D),
            Block.box(2.5D, 7.0D, 6.5D, 5.5D, 16.0D, 9.5D),
            Block.box(10.5D, 7.0D, 6.5D, 13.5D, 16.0D, 9.5D));

    public GrowLampBlock(Properties settings) {
        super(settings);
    }

    @Override
    public MapCodec<RedstoneLampBlock> codec() {
        return CODEC;
    }

    /** Vanilla's placement (lit if already powered), refused where there is no ceiling to hang from. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        return state != null && state.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? state : null;
    }

    /** The same test a hanging lantern makes: something under the block above to hang the chains from. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return Block.canSupportCenter(world, pos.above(), Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, RandomSource random) {
        return direction == Direction.UP && !state.canSurvive(world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Minecraft light has no colour, and a shader pack cannot be told a modded block's colour, so a
     * lit lamp shows its UV glow the one way every renderer draws: violet dust hanging in the air
     * under the panel. Client-side and cosmetic, at vanilla's own display-tick rate — the same hook
     * redstone ore sparkles on.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            world.addParticle(UV_GLOW,
                    pos.getX() + 0.1 + random.nextDouble() * 0.8,
                    pos.getY() + 0.2 - random.nextDouble() * 1.2,
                    pos.getZ() + 0.1 + random.nextDouble() * 0.8,
                    0.0, -0.02, 0.0);
        }
    }

    /** The diodes' violet, and a little smaller than redstone's dust. */
    private static final DustParticleOptions UV_GLOW = new DustParticleOptions(0x8A3CFF, 0.7F);
}
