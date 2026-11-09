package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Wild White Widow: a one-tile flower of Wild Purple Kush's kind that keeps the eyeblossom's hours.
 * Closed by day, its white buds shut; open at night, its orange hairs out. It reads the night exactly
 * as the eyeblossom does, vanilla's {@code gameplay/eyeblossom_open} at its own position, and turns on
 * a random tick with the eyeblossom's sounds.
 *
 * <p>The look and the sound only: it drops the same at any hour (its loot table), so no one has to
 * wait up for it.
 */
public class WhiteWidowFlowerBlock extends IndicaFlower {
    /** Open at night. Set explicitly in the default state: a boolean left out of it reads {@code true}. */
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public WhiteWidowFlowerBlock(Holder<MobEffect> stewEffect, float effectLengthInSeconds, Properties settings) {
        super(stewEffect, effectLengthInSeconds, settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(OPEN, opensAt(context.getLevel(), context.getClickedPos(), false));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        turn(state, world, pos);
    }

    /**
     * Opens or closes {@code state} at {@code pos} to match the night, with the eyeblossom's sound;
     * shared with the potted flower. Returns whether it turned.
     */
    static boolean turn(BlockState state, ServerLevel world, BlockPos pos) {
        boolean open = state.getValue(OPEN);
        boolean night = opensAt(world, pos, open);
        if (night == open) {
            return false;
        }
        world.setBlock(pos, state.setValue(OPEN, night), Block.UPDATE_ALL);
        world.playSound(null, pos, night ? SoundEvents.EYEBLOSSOM_OPEN : SoundEvents.EYEBLOSSOM_CLOSE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    /**
     * Whether a flower at {@code pos} should stand open: the eyeblossom's own attribute, which keeps
     * the flower as it is ({@code current}) wherever the attribute says nothing.
     */
    static boolean opensAt(LevelReader world, BlockPos pos, boolean current) {
        return world.environmentAttributes().getValue(EnvironmentAttributes.EYEBLOSSOM_OPEN, pos).toBoolean(current);
    }
}
