package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 *   <li>Its own seeds, and its own loot table — heavy on stems, next to no leaf, never a bud. It
 *       still flowers and smells of humulene when ripe, half as often as a bud crop: the wisp says
 *       the crop is ready, and every hemp crop says it the same way.</li>
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
     * The loader's farmland sum without the crowding halving. {@code getGrowthSpeed} halves when
     * a neighbour {@code is} the block it is handed, so it is handed one that never stands beside
     * a crop: void air only exists outside the world's height, and the neighbours are level with us.
     * NeoForge's also asks each soil whether it takes the plant handed over; farmland has no say of
     * its own there, so it counts as farmland on both loaders.
     */
    @Override
    public float fieldMoisture(LevelReader world, BlockPos pos) {
        return Services.PLATFORM.cropGrowthSpeed(Blocks.VOID_AIR.defaultBlockState(), world, pos);
    }

    @Override
    protected boolean canBeWorked() {
        return false;
    }

    @Override
    protected boolean canBeStressed() {
        return false;
    }

    /** No bud for mildew to spoil, and no shears to clear it with. */
    @Override
    public boolean catchesMildew() {
        return false;
    }

    /**
     * A ripe field smells, fainter than a bud crop: one sampling in {@link RipeAroma#FAINT_CHANCE}.
     * Reads {@code AGE} itself, as the Lemon Haze crop does (every segment mirrors the root's age).
     * No mildew to show: the fibre crop never catches it.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= this.getMaxAge()) {
            RipeAroma.randomDisplayTick(world, pos, random, Direction.UP, RipeAroma.FAINT_CHANCE);
        }
    }
}
