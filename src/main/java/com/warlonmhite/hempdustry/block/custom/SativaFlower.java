package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * Wild Lemon Haze. Like {@code IndicaFlower} its seed drop lives in the loot table, not here —
 * the one thing this class exists for is the ground it will grow on.
 *
 * <p>Plain {@link FlowerBlock} inherits {@code PlantBlock}'s rule that a flower may only sit on
 * {@code #minecraft:dirt} or farmland. Badlands is red sand and terracotta, so a stock flower can
 * <em>never</em> generate there — the badlands tier of the worldgen would silently place nothing.
 * Widening the rule to {@code #minecraft:dead_bush_may_place_on} (sand + terracotta + dirt) is the
 * same override {@code DeadBushBlock} uses, and it is what makes Lemon Haze an arid-ground plant:
 * a hardy landrace sativa scraping a living out of badlands, rather than another meadow flower.
 */
public class SativaFlower extends FlowerBlock {

    public SativaFlower(RegistryEntry<StatusEffect> stewEffect, float effectLengthInSeconds, Settings settings) {
        super(stewEffect, effectLengthInSeconds, settings);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(BlockTags.DEAD_BUSH_MAY_PLACE_ON) || super.canPlantOnTop(floor, world, pos);
    }
}
