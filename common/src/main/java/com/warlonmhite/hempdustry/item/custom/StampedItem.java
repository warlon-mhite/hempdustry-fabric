package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.PotencyBand;
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
        // THC-dominant on the prototype, so an explicit default stacks with a plain stack.
        super(settings.component(ModComponents.CHEMOTYPE, Chemotype.THC_DOMINANT));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Chemotype.of(stack).tooltipLine());
        // A leaf of a high or extra-high plant: its one mark. Nothing else stamped carries a band, and
        // the leaf's prototype carries mid, so the question is the value, never whether it is there.
        if (PotencyBand.of(stack) != PotencyBand.MID) {
            tooltip.accept(PotencyBand.of(stack).tooltipLine());
        }
        if (this instanceof Resin) {
            tooltip.accept(BudTier.of(stack).tooltipLine());
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    /**
     * Kief, bubble hash, filtered kief and what the Hemp Press makes of them (hashish, filtered hashish,
     * rosin): resin that carries the tier of the batch it was sifted in ({@link BudTier}), Dank on its
     * prototype so an explicit Dank stacks with a plain piece. Charas is rubbed off a living plant, before
     * any harvest has a tier, and is plain stamped matter.
     */
    public static class Resin extends StampedItem {
        public Resin(Properties settings) {
            super(settings.component(ModComponents.BUD_TIER, BudTier.DANK));
        }
    }
}
