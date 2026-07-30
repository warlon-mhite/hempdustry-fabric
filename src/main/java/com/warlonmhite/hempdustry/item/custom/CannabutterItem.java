package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Cannabutter. Inert for now — what it actually <em>does</em> belongs to the effects pass — but it
 * already carries the two things the Infuser decided about it, and the tooltip is the only place a
 * player can read them.
 *
 * <p>This is a deliberate exception to the "you are not wearing a HUD" principle rather than an
 * oversight. That principle is about not putting a stat panel where an in-world signal would do,
 * and the in-world signals are already carrying their share: the Infuser's bar shows patience and
 * its liquid tint shows dose while the batch is cooking. Once the butter is a stack in your
 * inventory there is no in-world surface left to read, and vanilla's own answer for exactly this
 * case is a tooltip line — potions, enchanted books and suspicious stew all do it.
 *
 * <p>Strength is shown as a plain count rather than a tier name because the tier thresholds live in
 * the effects pass and don't exist yet; showing the raw number now is honest and won't have to be
 * unlearned.
 */
public class CannabutterItem extends Item {

    public CannabutterItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        Quality quality = stack.get(ModComponents.QUALITY);
        if (quality != null) {
            tooltip.add(Text.translatable(quality.getTranslationKey()).formatted(colourOf(quality)));
        }
        Integer strength = stack.get(ModComponents.STRENGTH);
        if (strength != null) {
            tooltip.add(Text.translatable("item.hempdustry.cannabutter.strength", strength)
                    .formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    /** Climbs the same way vanilla's rarity colours do, so the ladder reads without being labelled. */
    private static Formatting colourOf(Quality quality) {
        return switch (quality) {
            case ROUGH -> Formatting.GRAY;
            case STANDARD -> Formatting.WHITE;
            case CLEAN -> Formatting.AQUA;
            case PERFECT -> Formatting.LIGHT_PURPLE;
        };
    }
}
