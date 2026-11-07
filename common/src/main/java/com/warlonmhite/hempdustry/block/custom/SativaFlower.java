package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.BlockTags;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wild Lemon Haze — <b>two blocks tall</b>, and the only wild plant in the mod that is.
 *
 * <p>Like {@code IndicaFlower} its seed drop lives in the loot table, not here. This class exists for
 * two things: the ground it will grow on, and the fact that it is a double plant at all.
 *
 * <h2>Why it is tall, and why that is not "it became a crop"</h2>
 *
 * The wild flowers recycle their strain's <b>mid-growth</b> crop art — Purple Kush takes
 * {@code indica_crop_stage3}, Lemon Haze takes {@code sativa_crop_stage5}. Purple Kush's stage 3 is a
 * single tile and stays a single block; Lemon Haze's is drawn across a bottom and a top, because
 * Lemon Haze is the lanky strain and reads wrong squashed into one block. <b>That is the whole reason
 * for the height</b> — a wild plant is not a crop and inherits none of a crop's behaviour: no age, no
 * defoliation, no farmland, no bees, no bone meal.
 *
 * <p>{@link DoublePlantBlock}, not {@code TallFlowerBlock}, is deliberate. The latter is
 * {@code BonemealableBlock} — bone meal on a rose bush drops a second rose bush — which here would be a
 * free route to a flower and therefore to seeds, bypassing the crop entirely.
 *
 * <p>It also drops the stew effect {@code FlowerBlock}'s constructor used to demand. That argument
 * was always dead ({@code ModBlocks}, and {@code crops.md}); a {@code DoublePlantBlock} never asks for it,
 * which is one fewer unreachable knob to explain.
 *
 * <h2>The ground rule, unchanged</h2>
 *
 * {@code BushBlock} lets a plant sit only on {@code #minecraft:dirt} or farmland. Badlands is red
 * sand and terracotta, so a stock plant can <em>never</em> generate there — the badlands tiers of the
 * worldgen would silently place nothing. Widening to {@code #minecraft:dead_bush_may_place_on}
 * (sand + terracotta + dirt) is the same override {@code DeadBushBlock} uses, and it is what makes
 * Lemon Haze an arid-ground plant: a hardy landrace sativa scraping a living out of badlands, rather
 * than another meadow flower.
 *
 * <p>{@code DoublePlantBlock#canSurvive} only consults this for the <b>lower</b> half; the upper half's
 * test is "is the lower half of me directly below", which needs no help.
 */
public class SativaFlower extends DoublePlantBlock {

    public SativaFlower(Properties settings) {
        super(settings);
    }

    /**
     * The dead bush's ground: 1.18.2 has no {@code #dead_bush_may_place_on} (1.20 made one of exactly
     * this), so the list in vanilla 1.18.2's {@code DeadBushBlock#mayPlaceOn} is written out -- sand, red
     * sand, the seventeen terracottas, and {@code #minecraft:dirt}.
     */
    private static final Set<Block> DEAD_BUSH_GROUND = Set.of(Blocks.SAND, Blocks.RED_SAND, Blocks.TERRACOTTA,
            Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
            Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA);

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return DEAD_BUSH_GROUND.contains(floor.getBlock()) || floor.is(BlockTags.DIRT)
                || super.mayPlaceOn(floor, world, pos);
    }
}
