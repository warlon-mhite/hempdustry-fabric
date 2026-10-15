package com.warlonmhite.hempdustry.item.custom;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Any edible carrying a dose — the toast, cookie, brownie, dawamesk and bhang. Eating one queues the
 * staggered bundle in {@link EdibleEffects} rather than applying anything immediately.
 *
 * <p>An edible with no {@code potency} component does nothing at all, which is what makes a
 * creative-tab or {@code /give} copy safe and is also why the hemp-seed foods are ordinary
 * {@link Item}s rather than this class.
 *
 * <p>The tooltip is the same narrow exception cannabutter takes: in-world signals carry the load
 * while the effect is running (the ramp itself tells you it landed), but a stack sitting in your
 * inventory has no in-world surface, and vanilla's own answer for that is a tooltip line — potions,
 * enchanted books and suspicious stew all do it.
 */
public class EdibleItem extends Item {

    public EdibleItem(Properties settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        int potency = EdibleEffects.potencyOf(stack);
        Quality quality = EdibleEffects.qualityOf(stack);
        // Read before super, which may empty the stack and take the components with it.
        ItemStack result = super.finishUsingItem(stack, world, user);
        if (!world.isClientSide && user instanceof Player player) {
            EdibleEffects.consume(player, potency, quality);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        EdibleEffects.appendTooltip(stack, tooltip);
    }
}
