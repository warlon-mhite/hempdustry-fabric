package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Warped Kush, the Nether's cannabis. Two tall, so it is the Purple Kush crop playing by nether
 * wart's rules instead of wheat's — the rest (halves, headroom, bees, whole-plant harvest,
 * defoliation) is shared, traps included.
 *
 * <ul>
 *   <li><b>Warped nylium, and nothing else.</b> No farmland, no Grow Pot, no Hydro Tray.</li>
 *   <li><b>No light, no water, no bone meal.</b> It neither needs light to survive nor to grow,
 *       reads no moisture and takes no crowding penalty, and refuses bone meal as nether wart does.</li>
 *   <li><b>Its speed is the world's.</b> 1-in-5 per random tick (~40 min seed to ripe) wherever
 *       water boils away — the Nether, or a datapack's hot dimension — and 1-in-20 (~160 min)
 *       anywhere else. The same vanilla attribute the Hydro Tray reads, mirrored.</li>
 *   <li><b>No light record.</b> The ladder never applies; the record stays {@code natural}.</li>
 * </ul>
 *
 * <p>The wild plant is this block, generated ripe on warped nylium. See {@code crops.md}.
 */
public class WarpedKushCropBlock extends IndicaCropBlock {
    /** {@code floor(resistance / 1) + 1}: 1-in-5 where water boils away, 1-in-20 elsewhere. */
    private static final float NETHER_RESISTANCE = 4.0F;
    private static final float ELSEWHERE_RESISTANCE = 19.0F;

    public WarpedKushCropBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.WARPED_NYLIUM);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.WARPED_KUSH_SEEDS;
    }

    @Override
    protected boolean needsLight() {
        return false;
    }

    @Override
    public float growthResistance(LevelReader world, BlockPos pos) {
        return HydroTrayBlock.boilsAway(world, pos) ? NETHER_RESISTANCE : ELSEWHERE_RESISTANCE;
    }

    /** Nylium is not farmland: a flat 1, so the resistance alone sets the odds and nothing crowds. */
    @Override
    public float fieldMoisture(LevelReader world, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean recordsLight() {
        return false;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return false;
    }
}
