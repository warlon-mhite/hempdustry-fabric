package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
