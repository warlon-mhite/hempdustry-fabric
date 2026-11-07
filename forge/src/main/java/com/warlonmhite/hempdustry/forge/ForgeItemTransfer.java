package com.warlonmhite.hempdustry.forge;

import com.warlonmhite.hempdustry.platform.ItemTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Forge's item handlers. All or nothing the way they allow it: the whole stack is offered as a
 * simulation first, and only a storage that would take all of it is given it -- both on the server
 * thread, with nothing in between. Vanilla's containers answer through it too; the mod's own machines
 * are added to it where the mod starts ({@code HempdustryForge}).
 */
public final class ForgeItemTransfer implements ItemTransfer {
    @Override
    public Result insertWhole(Level level, BlockPos pos, Direction side, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        IItemHandler handler = blockEntity == null ? null
                : blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
        if (handler == null) {
            return Result.NO_STORAGE;
        }
        if (!ItemHandlerHelper.insertItemStacked(handler, stack.copy(), true).isEmpty()) {
            return Result.FULL;
        }
        ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
        return Result.MOVED;
    }
}
