package com.warlonmhite.hempdustry.item.custom;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * The item form of a dosed block — the Space Cake. Exists only to show the same potency and quality
 * lines an {@link EdibleItem} shows; the dose itself moves into the blockstate on placement, which
 * {@code SpaceCakeBlock} handles.
 */
public class EdibleBlockItem extends BlockItem {
    public EdibleBlockItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                              TooltipDisplay displayComponent, Consumer<Component> tooltip,
                              TooltipFlag type) {
        EdibleEffects.appendTooltip(stack, tooltip);
    }
}
