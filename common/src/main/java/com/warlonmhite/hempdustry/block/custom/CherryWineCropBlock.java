package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.level.ItemLike;

/**
 * Cherry Wine — the CBD plant, bred from hemp for its flower. Purple Kush's body in every rule: two
 * tall, farmland or one of the mod's beds, the same growth, trims, light record and mildew. Its wild
 * plant is a flower of its own, as Purple Kush's is ({@code ModBlocks.CHERRY_WINE_FLOWER}).
 */
public class CherryWineCropBlock extends IndicaCropBlock {

    public CherryWineCropBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.CHERRY_WINE_SEEDS;
    }
}
