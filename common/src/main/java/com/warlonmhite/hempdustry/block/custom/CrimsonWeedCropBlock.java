package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crimson Weed, the crimson forest's cannabis, and the one plant in the mod that <b>hangs</b>. Two
 * tall like Purple Kush, but grown from the underside of a block and downward, the way weeping vines
 * grow — so its root is the UPPER half, and the tip dangles below it ({@link IndicaCropBlock#hangs}).
 * Everything else the crop class does (headroom, bees, whole-plant harvest, defoliation) holds with
 * the halves swapped.
 *
 * <ul>
 *   <li><b>{@code #hempdustry:crimson_canopy}, and nothing else</b> — the nether wart block, the
 *       crimson forest's canopy, and never the warped wart block, which is Warped Kush's forest. A
 *       pack may widen the tag. Since it needs a ceiling, it never stands in a Grow Pot or a Hydro
 *       Tray: a bed sits on the floor.</li>
 *   <li><b>No light, no water, no bone meal</b>, as Warped Kush: it reads no moisture, takes no
 *       crowding penalty and keeps no light record.</li>
 *   <li><b>It grows only where water boils away</b> — the Nether, or a datapack's hot dimension —
 *       at Warped Kush's 1-in-5 per random tick. Anywhere else it can be hung and lives, but never
 *       grows: the harder plant to reach, not the slower one to grow.</li>
 *   <li><b>No room below, no tip.</b> The tip needs air: over lava, water or a block it stops one
 *       stage short of needing one, like any tall plant under a roof.</li>
 * </ul>
 *
 * <p>The wild plant is this block, generated ripe under the canopy of crimson forests.
 */
public class CrimsonWeedCropBlock extends IndicaCropBlock {
    /** {@code floor(4 / 1) + 1}: 1-in-5 a random tick where water boils away, Warped Kush's rate. */
    private static final float NETHER_RESISTANCE = 4.0F;

    public CrimsonWeedCropBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected boolean hangs() {
        return true;
    }

    /** The block above the root: a canopy block, never a floor or a bed. */
    @Override
    protected boolean mayPlaceOn(BlockState ceiling, BlockGetter world, BlockPos pos) {
        return ceiling.is(ModTags.Blocks.CRIMSON_CANOPY);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.CRIMSON_SEEDS;
    }

    @Override
    protected boolean needsLight() {
        return false;
    }

    @Override
    public float growthResistance(LevelReader world, BlockPos pos) {
        return NETHER_RESISTANCE;
    }

    /** A flat 1 where water boils away, so nothing crowds it; {@code 0} — no growth — anywhere else. */
    @Override
    public float fieldMoisture(LevelReader world, BlockPos pos) {
        return HydroTrayBlock.boilsAway(world, pos) ? 1.0F : 0.0F;
    }

    @Override
    protected boolean recordsLight() {
        return false;
    }

    /**
     * No mildew, as Warped Kush. The Nether is too hot and dry for it, and a plant hanging from a
     * canopy would always count as roofed.
     */
    @Override
    public boolean catchesMildew() {
        return false;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, BonemealSource source) {
        return false;
    }
}
