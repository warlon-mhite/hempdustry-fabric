package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.level.ItemLike;

/**
 * Dud Weed — the cannabinoid-free plant, bred out of fibre hemp. Purple Kush's body in every rule (two
 * tall, farmland or one of the mod's beds, the same growth, trims, light record and mildew), drawn
 * with the fibre family's lanky outline. It grows nowhere wild: it turns up in a field of ripe
 * Industrial Hemp, as a sport, and in shipwrecks' stores. Its harvest is the seed crop's: more seeds,
 * fewer leaves.
 */
public class DudWeedCropBlock extends IndicaCropBlock {

    public DudWeedCropBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.DUD_WEED_SEEDS;
    }
}
