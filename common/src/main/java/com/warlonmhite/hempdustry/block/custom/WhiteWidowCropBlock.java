package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.level.ItemLike;

/**
 * White Widow — the pale garden's plant, white with resin. Purple Kush's body in every rule (two tall,
 * farmland or one of the mod's beds, the same growth, trims and mildew), with one difference that is
 * the whole strain: it is touchy about its light. A lit light over it at night, or a light-15 block
 * beside it, stresses it once it is three stages old, and a lost light by day stresses it a stage
 * early ({@link PlantStress#touchyStep}). Its wild plant is a flower that opens at night
 * ({@link WhiteWidowFlowerBlock}).
 */
public class WhiteWidowCropBlock extends IndicaCropBlock {

    public WhiteWidowCropBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.WHITE_WIDOW_SEEDS;
    }

    @Override
    protected boolean leaksAtNight() {
        return true;
    }
}
