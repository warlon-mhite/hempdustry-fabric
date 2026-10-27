package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Cannabutter. Inert for now — what it actually <em>does</em> belongs to the effects pass — but it
 * already carries the two things the Infuser decided about it, and the tooltip is the only place a
 * player can read them.
 *
 * <p>This is a deliberate exception to the "you are not wearing a HUD" principle rather than an
 * oversight. That principle is about not putting a stat panel where an in-world signal would do,
 * and the in-world signals are already carrying their share: the Infuser's bar shows patience
 * while the batch is cooking (its liquid shows no dose; a second brew colour is drawn but not
 * wired). Once the butter is a stack in your inventory there is no in-world surface left to read, and vanilla's own answer for exactly this
 * case is a tooltip line — potions, enchanted books and suspicious stew all do it.
 *
 * <p>Strength is shown as a plain count rather than a tier name because the tier thresholds live in
 * the effects pass and don't exist yet; showing the raw number now is honest and won't have to be
 * unlearned.
 */
public class CannabutterItem extends Item {

    public CannabutterItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                              TooltipDisplay displayComponent, Consumer<Component> tooltip,
                              TooltipFlag type) {
        Quality quality = stack.get(ModComponents.QUALITY);
        if (quality != null) {
            tooltip.accept(Component.translatable(quality.getTranslationKey()).withStyle(colourOf(quality)));
        }
        Integer strength = stack.get(ModComponents.STRENGTH);
        if (strength != null) {
            tooltip.accept(Component.translatable("item.hempdustry.cannabutter.strength", strength)
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, displayComponent, tooltip, type);
    }

    /** Climbs the same way vanilla's rarity colours do, so the ladder reads without being labelled. */
    private static ChatFormatting colourOf(Quality quality) {
        return switch (quality) {
            case ROUGH -> ChatFormatting.GRAY;
            case STANDARD -> ChatFormatting.WHITE;
            case CLEAN -> ChatFormatting.AQUA;
            case PERFECT -> ChatFormatting.LIGHT_PURPLE;
        };
    }
}
