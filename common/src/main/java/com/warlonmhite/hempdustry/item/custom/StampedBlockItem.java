package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.block.custom.ChemotypeBlocks;
import com.warlonmhite.hempdustry.block.custom.HempLeavesBlock;
import com.warlonmhite.hempdustry.block.custom.TierBlocks;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * The item of a block of nine stamped pieces ({@link ChemotypeBlocks}): its chemotype travels as the
 * block state it will be placed with, and its tooltip names it like a {@link StampedItem}'s.
 */
public class StampedBlockItem extends BlockItem {

    public StampedBlockItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(ChemotypeBlocks.ofBlockItem(stack).tooltipLine());
        if (HempLeavesBlock.markOfBlockItem(stack).marksLeaf()) {
            tooltip.accept(PotencyBand.HIGH.tooltipLine());
        }
        if (TierBlocks.holdsTier(stack)) {
            tooltip.accept(TierBlocks.ofBlockItem(stack).tooltipLine());
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
