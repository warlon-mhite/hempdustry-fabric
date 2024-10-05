package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.sound.ModSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.World;


public class IndicaSpliffItem extends Item {
    private static final int COOLDOWN_TICKS = 80;

    public IndicaSpliffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (!player.getItemCooldownManager().isCoolingDown(this)) {
            if (!world.isClient) {
                itemStack.decrement(1);
                if (itemStack.isEmpty()) {
                    player.setStackInHand(hand, ItemStack.EMPTY);
                }

                world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SMOKING, SoundCategory.PLAYERS, 1f, 1f);
                if (ThreadLocalRandom.current().nextInt(6) == 0) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.COUGHING, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
                player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
                return TypedActionResult.success(itemStack, world.isClient());
                } else {
                return TypedActionResult.pass(itemStack);
            }
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }
}