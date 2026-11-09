package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.strain.Chemotype;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Hemp matter that is the same item whatever plant it came from — a leaf, schwag, decarboxylated and
 * scorched hemp, the hash family — and so carries its plant's chemotype as a stamp
 * ({@code hempdustry:chemotype}). Its tooltip names the chemotype in a word, THC-dominant included:
 * an unstamped stack is THC-dominant, and saying so is what tells it apart from a CBD one beside it.
 */
public class StampedItem extends Item {

    public StampedItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Chemotype.of(stack).tooltipLine());
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
