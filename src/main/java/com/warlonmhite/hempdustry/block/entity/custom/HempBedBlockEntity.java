package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

/**
 * Holds nothing. A bed's block entity exists only so a renderer can draw it, and vanilla's
 * {@code BedBlockEntity} is tied to its own type and its sixteen dye textures — this is the same
 * thing under our type, so the hemp bed's renderer is the one that finds it.
 */
public class HempBedBlockEntity extends BlockEntity {
    public HempBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEMP_BED, pos, state);
    }

    // Vanilla's bed sends itself on every update, so a client never has a bed with nothing to draw.
    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
