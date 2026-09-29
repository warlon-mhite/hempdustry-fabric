package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

/**
 * A bed made up in hemp cloth — the seventeenth bed, beside vanilla's sixteen dyed ones.
 *
 * <p>Vanilla's bed carries its colour as a {@link DyeColor}, and there is no seventeenth one. The
 * colour is read in exactly one place, {@code BedBlockEntity}, to pick one of sixteen fixed textures
 * (checked in the 1.21.11 bytecode: nothing else calls {@code getColor}). So everything a bed
 * <em>does</em> — sleeping, the spawn point, blowing up in the Nether, one drop from two halves —
 * is inherited as it is, and only the block entity is ours: {@link HempBedBlockEntity}, drawn with
 * the hemp texture by its own renderer. {@code WHITE} is what the unused colour says, because hemp
 * cloth is undyed and white is vanilla's undyed wool.
 */
public class HempBedBlock extends BedBlock {
    public HempBedBlock(Settings settings) {
        super(DyeColor.WHITE, settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HempBedBlockEntity(pos, state);
    }
}
