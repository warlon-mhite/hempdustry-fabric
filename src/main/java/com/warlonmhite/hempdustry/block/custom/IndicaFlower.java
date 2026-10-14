package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.FlowerBlock;

/**
 * Wild indica flower. Its seed drop (with Fortune scaling) is defined in the
 * loot table (see ModLootTableProvider), not here.
 */
public class IndicaFlower extends FlowerBlock {

    public IndicaFlower(Holder<MobEffect> stewEffect, float effectLengthInSeconds, Properties settings) {
        super(stewEffect, effectLengthInSeconds, settings);
    }
}
