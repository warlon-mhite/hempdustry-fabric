package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Beldía — the Rif's landrace, grown for the sieve. A Purple Kush-shaped plant with one different
 * rule: <b>it grows on sand, beside water</b>, and never on farmland.
 *
 * <h2>Water: farmland's reach, farmland's forgiveness</h2>
 *
 * The water it needs is anywhere in <b>farmland's own hydration box</b> around the sand — four blocks
 * out, level with the sand or one above ({@code FarmBlock#isNearWater}) — so there is one rule
 * for a player to learn, not two. It is asked in two places and <b>never in {@code canSurvive}</b>:
 *
 * <ul>
 *   <li><b>on planting</b> ({@link #getStateForPlacement}), so a seed only goes down where it can grow;</li>
 *   <li><b>on the random tick</b> ({@link #fieldMoisture}), so a plant whose water is taken away
 *       stops growing, and starts again when it comes back.</li>
 * </ul>
 *
 * <p>Survival reads the block below and nothing else. Sugar cane pops the moment its water goes,
 * but it only ever looks one block away: a neighbour update cannot see water removed four blocks
 * out, so the same pop would fire at once at one block and minutes later at four, which reads as a
 * bug. A popping two-block plant would also roll its loot twice ({@code crops.md}). Dry farmland
 * does not kill a crop either — it only stops helping it.
 *
 * <p>In a Grow Pot or a Hydro Tray the bed answers for itself, as it does for every plant: a seed goes
 * down in either with no water in reach, and a dry pot or tray slows and stresses Beldía exactly as it
 * does any other strain. The desert's forgiveness is the sand's, not the plant's.
 */
public class BeldiaCropBlock extends IndicaCropBlock {

    /** Farmland's hydration reach, horizontally ({@code FarmBlock#isNearWater}). */
    public static final int WATER_REACH = 4;

    /** A lone plant on sand with water in reach reads as a lone plant on wet farmland. */
    private static final float WATERED_MOISTURE = 10.0F;

    public BeldiaCropBlock(Properties settings) {
        super(settings);
    }

    /** Anything in {@code #hempdustry:beldia_soil} (sand, red sand, any mod's {@code #c:sands}), or a bed of ours. */
    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(ModTags.Blocks.BELDIA_SOIL) || floor.getBlock() instanceof GrowMedium;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.BELDIA_SEEDS;
    }

    /** A seed refuses sand with no water in reach — {@code null} is how a block item is told no. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return isWatered(ctx.getLevel(), ctx.getClickedPos().below()) ? super.getStateForPlacement(ctx) : null;
    }

    /** Bone meal is growth too, and a dry plant does not grow. */
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return super.isValidBonemealTarget(world, pos, state) && isWatered(world, pos.below());
    }

    /**
     * Wet farmland's number when the sand has water in reach, and {@code 0} — no growth this tick —
     * when it has none.
     *
     * <p>The crowding penalty is vanilla's own, read through {@code getGrowthSpeed} (the loader's): on sand no
     * farmland contributes, so that method returns exactly its base of 1, halved to 0.5 when the
     * plant is crowded — and this scales it up to wet farmland's 10 or 5. A field of Beldía therefore
     * wants rows exactly as a field of wheat does.
     */
    @Override
    public float fieldMoisture(LevelReader world, BlockPos pos) {
        if (!isWatered(world, pos.below())) {
            return 0.0F;
        }
        // ponytail: reads 1 or 0.5 only while the neighbours are sand; farmland beside a Beldía row
        // adds to it and hides the crowding halving. Count the sand ourselves if that ever matters.
        return Services.PLATFORM.cropGrowthSpeed(defaultBlockState(), world, pos) < 1.0F ? WATERED_MOISTURE / 2 : WATERED_MOISTURE;
    }

    /** Whether the ground at {@code floor} counts as watered: a bed of ours answers for itself, sand needs water in reach. */
    public static boolean isWatered(LevelReader world, BlockPos floor) {
        if (world.getBlockState(floor).getBlock() instanceof GrowMedium) {
            return true;
        }
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-WATER_REACH, 0, -WATER_REACH),
                floor.offset(WATER_REACH, 1, WATER_REACH))) {
            if (world.getFluidState(pos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
