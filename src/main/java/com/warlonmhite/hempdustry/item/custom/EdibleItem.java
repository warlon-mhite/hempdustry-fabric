package com.warlonmhite.hempdustry.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

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

    public EdibleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        int potency = EdibleEffects.potencyOf(stack);
        Quality quality = EdibleEffects.qualityOf(stack);
        // Read before super, which may empty the stack and take the components with it.
        ItemStack result = super.finishUsing(stack, world, user);
        if (world instanceof ServerWorld serverWorld && user instanceof PlayerEntity player) {
            EdibleEffects.consume(serverWorld, player, potency, quality);
        }
        return result;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        EdibleEffects.appendTooltip(stack, tooltip);
    }
}
