package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Block of Hemp Leaves: nine leaves of one chemotype, and of one mark — nine leaves of a high or
 * extra-high plant make a block marked {@link #HIGH_POTENCY}, and break back into nine marked leaves.
 * The mark is a second state beside the chemotype (ten states), carried by the block item the way the
 * chemotype is, through vanilla's {@code minecraft:block_state} component. A block 2.1 placed has no
 * mark, and none is what it was.
 */
public class HempLeavesBlock extends StampedBlock {
    public static final BooleanProperty HIGH_POTENCY = BooleanProperty.create("high_potency");

    public HempLeavesBlock(Properties settings) {
        super(settings);
        // Set explicitly: a BooleanProperty left out of the default state resolves to true.
        registerDefaultState(defaultBlockState().setValue(HIGH_POTENCY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HIGH_POTENCY);
    }

    /** A creative pick-block keeps the mark too, beside the chemotype {@link StampedBlock} keeps. */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return markBlockItem(super.getCloneItemStack(level, pos, state, includeData),
                state.getValue(HIGH_POTENCY) ? PotencyBand.HIGH : PotencyBand.MID);
    }

    /** The mark a block item will place with: high when its state says so, else mid. */
    public static PotencyBand markOfBlockItem(ItemStack stack) {
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        return properties != null && Boolean.TRUE.equals(properties.get(HIGH_POTENCY)) ? PotencyBand.HIGH : PotencyBand.MID;
    }

    /** Marks a block item to place high or not; unmarked writes nothing, so it stacks with 2.1's. */
    public static ItemStack markBlockItem(ItemStack stack, PotencyBand band) {
        BlockItemStateProperties properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        java.util.Map<String, String> kept = new java.util.HashMap<>(properties.properties());
        if (band.marksLeaf()) {
            kept.put(HIGH_POTENCY.getName(), "true");
        } else {
            kept.remove(HIGH_POTENCY.getName());
        }
        if (kept.isEmpty()) {
            stack.remove(DataComponents.BLOCK_STATE);
        } else {
            stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(kept));
        }
        return stack;
    }
}
