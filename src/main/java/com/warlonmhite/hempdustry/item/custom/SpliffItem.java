package com.warlonmhite.hempdustry.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A single-use, strain-specific joint: right-click to take one hit, then it's gone. Generalised
 * from the original {@code IndicaSpliffItem} so every strain reuses the same class.
 */
public class SpliffItem extends Item {
    private static final int COOLDOWN_TICKS = 80;
    private static final int COUGH_CHANCE_ONE_IN = 6;
    private static final int NAUSEA_CHANCE_ONE_IN = 500; // 0.2%

    private final Strain strain;

    public SpliffItem(Strain strain, Settings settings) {
        super(settings);
        this.strain = strain;
    }

    public Strain strain() {
        return strain;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (!world.isClient) {
            Smoking.takeHit(world, player, strain, Potency.LIGHT, COUGH_CHANCE_ONE_IN, NAUSEA_CHANCE_ONE_IN);
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
