package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Industrial Hemp: the fibre crop. Three tall, like Lemon Haze, so it is that crop with four
 * things changed — the rest (segments, headroom, bees, whole-plant harvest, the light record) is
 * shared, traps included.
 *
 * <ul>
 *   <li><b>No crowding penalty.</b> Fibre hemp is sown about twenty times denser than drug hemp,
 *       because crowding is what grows a tall, unbranched stalk. So a solid field grows as fast as
 *       a single row, and that is where its speed comes from: vanilla's resistance of 25 (Purple
 *       Kush's) at twice the pace of a packed Purple Kush field.</li>
 *   <li><b>No trimming, no rubbing.</b> There are no buds to push a harvest towards and no resin
 *       worth taking. Shears fall through to vanilla.</li>
 *   <li><b>Farmland only.</b> The Grow Pot and the Hydro Tray are for buds; fibre is a field crop.</li>
 *   <li>Its own seeds, and its own loot table — heavy on stems, next to no leaf, never a bud. And
 *       with no bud to ripen, no smell when ripe.</li>
 *   <li><b>Never stressed.</b> Fibre cultivars are commonly bred monoecious — both sexes on one plant
 *       by design — so "it went hermaphrodite" means nothing for it. It keeps a light record, which
 *       its schwag reads; losing a lamp only lowers it.</li>
 * </ul>
 *
 * <p>Not a {@code Strain}: a strain is something you smoke, and it must name buds.
 */
public class IndustrialHempCropBlock extends SativaCropBlock {
    private static final float GROWTH_RESISTANCE = 25.0F;

    public IndustrialHempCropBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.FARMLAND);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.INDUSTRIAL_HEMP_SEEDS;
    }

    @Override
    protected float growthResistance() {
        return GROWTH_RESISTANCE;
    }

    /**
     * Vanilla's farmland sum without the crowding halving. {@code getGrowthSpeed} halves when
     * a neighbour {@code is} the block it is handed, so it is handed one that never stands beside
     * a crop: void air only exists outside the world's height, and the neighbours are level with us.
     */
    @Override
    public float fieldMoisture(LevelReader world, BlockPos pos) {
        return getGrowthSpeed(Blocks.VOID_AIR, world, pos);
    }

    @Override
    protected boolean canBeWorked() {
        return false;
    }

    @Override
    protected boolean canBeStressed() {
        return false;
    }

    /**
     * No smell when ripe. The wisp tells a player a plant's buds are ready, and fibre hemp has none,
     * so a ripe field of it stays as quiet as an unripe one.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
    }
}
