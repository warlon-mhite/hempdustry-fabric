package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * Beldía — the Rif's landrace, grown for the sieve. A Purple Kush-shaped plant with one different
 * rule: <b>it grows on sand, beside water</b>, and never on farmland.
 *
 * <h2>Water: farmland's reach, farmland's forgiveness</h2>
 *
 * The water it needs is anywhere in <b>farmland's own hydration box</b> around the sand — four blocks
 * out, level with the sand or one above ({@code FarmlandBlock#isWaterNearby}) — so there is one rule
 * for a player to learn, not two. It is asked in two places and <b>never in {@code canPlaceAt}</b>:
 *
 * <ul>
 *   <li><b>on planting</b> ({@link #getPlacementState}), so a seed only goes down where it can grow;</li>
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
 * <p>In a Grow Pot or a Hydro Tray the bed answers for itself, as it does for every plant: a pot is
 * always watered, and a tray has its own water level.
 */
public class BeldiaCropBlock extends IndicaCropBlock {

    /** Farmland's hydration reach, horizontally ({@code FarmlandBlock#isWaterNearby}). */
    public static final int WATER_REACH = 4;

    /** A lone plant on sand with water in reach reads as a lone plant on wet farmland. */
    private static final float WATERED_MOISTURE = 10.0F;

    public BeldiaCropBlock(Settings settings) {
        super(settings);
    }

    /** Any sand a mod calls sand ({@code #c:sands}, which leaves suspicious sand out), or a bed of ours. */
    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(ConventionalBlockTags.SANDS) || floor.getBlock() instanceof GrowMedium;
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.BELDIA_SEEDS;
    }

    /** A seed refuses sand with no water in reach — {@code null} is how a block item is told no. */
    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return isWatered(ctx.getWorld(), ctx.getBlockPos().down()) ? super.getPlacementState(ctx) : null;
    }

    /** Bone meal is growth too, and a dry plant does not grow. */
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return super.isFertilizable(world, pos, state) && isWatered(world, pos.down());
    }

    /**
     * Wet farmland's number when the sand has water in reach, and {@code 0} — no growth this tick —
     * when it has none.
     *
     * <p>The crowding penalty is vanilla's own, read through {@code getAvailableMoisture}: on sand no
     * farmland contributes, so that method returns exactly its base of 1, halved to 0.5 when the
     * plant is crowded — and this scales it up to wet farmland's 10 or 5. A field of Beldía therefore
     * wants rows exactly as a field of wheat does.
     */
    @Override
    public float fieldMoisture(WorldView world, BlockPos pos) {
        if (!isWatered(world, pos.down())) {
            return 0.0F;
        }
        // ponytail: reads 1 or 0.5 only while the neighbours are sand; farmland beside a Beldía row
        // adds to it and hides the crowding halving. Count the sand ourselves if that ever matters.
        return getAvailableMoisture(this, world, pos) < 1.0F ? WATERED_MOISTURE / 2 : WATERED_MOISTURE;
    }

    /** Whether the ground at {@code floor} is watered: a bed of ours always is, sand needs water in reach. */
    public static boolean isWatered(WorldView world, BlockPos floor) {
        if (world.getBlockState(floor).getBlock() instanceof GrowMedium) {
            return true;
        }
        for (BlockPos pos : BlockPos.iterate(floor.add(-WATER_REACH, 0, -WATER_REACH),
                floor.add(WATER_REACH, 1, WATER_REACH))) {
            if (world.getFluidState(pos).isIn(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
