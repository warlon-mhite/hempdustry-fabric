package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;

/**
 * A bed made up in hemp cloth — the seventeenth bed, beside vanilla's sixteen dyed ones.
 *
 * <p>Vanilla's bed carries its colour as a {@link DyeColor}, and there is no seventeenth one. Since
 * 26.2 a bed is drawn by its block model, and the colour picks nothing at all (nothing in the 26.2
 * game calls {@code getColor} but the block's own codec), so everything a bed <em>does</em> —
 * sleeping, the spawn point, blowing up in the Nether, one drop from two halves — is inherited as
 * it is, and only the models and their seven face textures are ours. {@code WHITE} is what the
 * unused colour says, because hemp cloth is undyed and white is vanilla's undyed wool.
 *
 * <p>Up to 1.21.11 a bed had a block entity, and the Hemp Bed had one of its own; a world brought
 * over from there loses it the way vanilla's beds lose theirs (see {@code DataFixersMixin}).
 */
public class HempBedBlock extends BedBlock {
    public HempBedBlock(Properties settings) {
        super(DyeColor.WHITE, settings);
    }
}
