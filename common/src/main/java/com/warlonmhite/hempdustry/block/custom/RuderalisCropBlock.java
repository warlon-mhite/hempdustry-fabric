package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ruderalis, the wild ancestor: one block tall, and the one plant in the mod that <b>nothing
 * accelerates</b>. A plain {@link CropBlock} rather than an {@link IndicaCropBlock}, because with no
 * second half, no trimming and no light record there is nothing of that class to inherit.
 *
 * <ul>
 *   <li><b>{@code #minecraft:sniffer_diggable_block}, and nothing else.</b> It grows exactly where a
 *       sniffer digs it up. That tag is {@code #minecraft:dirt} without mycelium, and farmland is in
 *       neither, so it cannot be tilled for — and a Grow Pot or a Hydro Tray is refused outright
 *       rather than accepted and ignored.</li>
 *   <li><b>A flat 1-in-4 per random tick</b> (~32 min seed to ripe), from a growth resistance of
 *       {@value #GROWTH_RESISTANCE} over a moisture of 1: there is no farmland under it to be moist,
 *       so it neither needs water nor minds being packed solid. Faster than a crowded Purple Kush
 *       field, slower than a tended one.</li>
 *   <li><b>No bone meal, and none eaten</b>; not in {@code #minecraft:crops}, so no bees.</li>
 *   <li><b>It grows in vanilla's light 9, and survives any light at all.</b> A weed of waste ground
 *       does not die because a tree grew over it: in the dark it simply stops, as a dry Beldía does.
 *       Growing in the dark would be another plant's trick — Warped Kush's — and false besides, since
 *       cannabis is one of the most light-hungry plants there is.</li>
 *   <li><b>No light record, no trimming, no charas.</b> A lamp over it changes nothing, and shears on
 *       it do nothing.</li>
 * </ul>
 *
 * <p>Its seeds are dug up — by a sniffer, or brushed out of trail ruins ({@code
 * ModLootTableModifiers}) — and a relict stand in the taigas, ripe and very rare, is luck
 * ({@code ModConfiguredFeatures}).
 */
public class RuderalisCropBlock extends CropBlock {
    /** {@code floor(3 / 1) + 1}: one random tick in four. */
    private static final float GROWTH_RESISTANCE = 3.0F;
    /** Vanilla's crop threshold, read the way {@code CropBlock#randomTick} reads it. */
    private static final int MIN_LIGHT_TO_GROW = 9;

    public RuderalisCropBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(BlockTags.SNIFFER_DIGGABLE_BLOCK);
    }

    /**
     * The soil alone. {@code CropBlock} pairs it with {@code hasSufficientLight}, which is what breaks
     * a vanilla crop off the moment its light drops below 8 — a wild plant under a spruce, or one a
     * player builds a roof over. This one stands and waits instead; {@link #randomTick} is where the
     * light is still required, so the dark costs it its growth and nothing else.
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos floor = pos.below();
        return this.mayPlaceOn(world.getBlockState(floor), world, floor);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.RUDERALIS_SEEDS;
    }

    /** Vanilla's roll with vanilla's moisture lookup taken out, through the shared growth roll. */
    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        int age = this.getAge(state);
        if (age < this.getMaxAge() && world.getRawBrightness(pos, 0) >= MIN_LIGHT_TO_GROW
                && HempGrowth.rolls(random, GROWTH_RESISTANCE, 1.0F, 1.0F)) {
            world.setBlock(pos, this.getStateForAge(age + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return false;
    }

    /**
     * A ripe plant smells like every other hemp crop; see {@link RipeAroma}. With one block there is
     * no upper half to lie about its age, so vanilla's {@code isMaxAge} is the plain answer, and the
     * helper's open-air test is the only one left.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (this.isMaxAge(state)) {
            RipeAroma.randomDisplayTick(world, pos, random);
        }
    }
}
