package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Wild indica flower. Its seed drop (with Fortune scaling) is defined in the
 * loot table (see ModLootTableProvider), not here.
 */
public class IndicaFlower extends FlowerBlock {

    public IndicaFlower(RegistryEntry<StatusEffect> stewEffect, float effectLengthInSeconds, Settings settings) {
        super(stewEffect, effectLengthInSeconds, settings);
    }
}
