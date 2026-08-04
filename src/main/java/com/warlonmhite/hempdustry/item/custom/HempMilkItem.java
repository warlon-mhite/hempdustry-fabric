package com.warlonmhite.hempdustry.item.custom;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Hemp milk — the seed milk, and <b>not</b> the same thing as {@link BhangItem}. Zero THC, no
 * relation to the drug side of the plant: it is what you get by boiling hemp seed until the husks
 * crack, pressing the white pulp through a sieve and discarding the husks, which is a real,
 * ordinary plant milk sold in supermarkets.
 *
 * <p>Mechanically it is <b>vanilla's milk bucket with a different source</b>: it clears every status
 * effect and it is deliberately <em>not food</em>. Both halves of that matter.
 *
 * <ul>
 *   <li><b>It clears effects</b> because a plant milk that didn't would be a trap — players reach for
 *       anything called milk when they want the cure, and punishing that reflex is bad design for no
 *       gain. Milk's effect-wipe is a Minecraft fiction rather than a property of dairy, so a plant
 *       milk having it is no less honest than a cow's.</li>
 *   <li><b>It is not food</b> because cow milk isn't either. Nutritious <em>and</em> curative would
 *       make it strictly better than the thing it substitutes for; with no nutrition it is a true
 *       sidegrade — same function, grown instead of milked. That is what a plant milk <em>is</em>,
 *       and it hands cow-free and skyblock-style players a route vanilla gates behind livestock.</li>
 * </ul>
 *
 * <p>There is a joke in here that lands better than the one attached to bhang: in a hemp mod, you
 * cure your own high with hemp.
 */
public class HempMilkItem extends Item {
    /** Vanilla's milk bucket drink time. */
    private static final int MAX_USE_TIME = 32;

    public HempMilkItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity player) {
            Criteria.CONSUME_ITEM.trigger(player, stack);
            player.incrementStat(Stats.USED.getOrCreateStat(this));
        }
        if (!world.isClient) {
            user.clearStatusEffects();
        }
        // exchangeStack wants a PlayerEntity; anything else that somehow drinks this just loses it.
        if (!(user instanceof PlayerEntity player)) {
            return stack;
        }
        return ItemUsage.exchangeStack(stack, player, new ItemStack(Items.BUCKET));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_USE_TIME;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
}
