package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.registry.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.FlowerBlock;

/**
 * Wild indica flower. Its seed drop (with Fortune scaling) is defined in the
 * loot table (see ModLootTableProvider), not here.
 */
public class IndicaFlower extends FlowerBlock {

    public IndicaFlower(MobEffect stewEffect, int effectLengthInSeconds, Properties settings) {
        super(stewEffect, effectLengthInSeconds, settings);
    }
}
