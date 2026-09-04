package com.warlonmhite.hempdustry.item.custom;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * The item form of a dosed block — the Space Cake. Exists only to show the same potency and quality
 * lines an {@link EdibleItem} shows; the dose itself moves into the blockstate on placement, which
 * {@code SpaceCakeBlock} handles.
 */
public class EdibleBlockItem extends BlockItem {
    public EdibleBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent, Consumer<Text> tooltip,
                              TooltipType type) {
        EdibleEffects.appendTooltip(stack, tooltip);
    }
}
