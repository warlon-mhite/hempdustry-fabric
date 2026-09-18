package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

/**
 * A bong is drawn, not clicked: the hit lands at the end of the hold, and letting go spends nothing.
 *
 * <p>Two quiet failures. A draw that hit on the click as well as at the end would be two hits for
 * one — the generous direction nobody reports. And a release that still spent a charge would make
 * the cancel a trap that looks like lag. The pipe is the control: it must still hit on the click,
 * or the draw has leaked into every device.
 */
public final class BongRipGameTest {

    public static void bongIsDrawnNotClicked(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        ItemStack bong = packed(world, ModItems.BONG, DeviceType.BONG);
        player.setStackInHand(Hand.MAIN_HAND, bong);
        player.interactionManager.interactItem(player, world, bong, Hand.MAIN_HAND);
        context.assertTrue(player.isUsingItem(), "clicking a packed bong did not start a draw");
        context.assertTrue(player.getActiveItem().getUseAction() == UseAction.TOOT_HORN,
                "a bong draw is not raised to the mouth");
        context.assertEquals(charges(player), DeviceType.BONG.bowlSize(), "the click alone took a hit");

        player.stopUsingItem();
        context.assertEquals(charges(player), DeviceType.BONG.bowlSize(), "letting go early spent a charge");
        context.assertTrue(player.getStackInHand(Hand.MAIN_HAND).getDamage() == 0,
                "letting go early cost durability");

        player.interactionManager.interactItem(player, world, player.getStackInHand(Hand.MAIN_HAND), Hand.MAIN_HAND);
        // What LivingEntity#consumeItem does at the end of a hold: finish, then let go. Skipping the
        // second leaves the player "using", and the pipe below would then prove nothing.
        ItemStack after = player.getActiveItem().finishUsing(world, player);
        player.clearActiveItem();
        player.setStackInHand(Hand.MAIN_HAND, after);
        context.assertEquals(charges(player), DeviceType.BONG.bowlSize() - 1, "a finished draw did not take one hit");
        context.assertTrue(player.getItemCooldownManager().isCoolingDown(after),
                "a finished draw started no cooldown");

        // The control: the pipe still hits on the click, and never enters a draw. A fresh player,
        // because the cooldown the bong just started covers every smokeable, the pipe included.
        ServerPlayerEntity other = context.createMockCreativeServerPlayerInWorld();
        other.changeGameMode(GameMode.SURVIVAL);
        ItemStack pipe = packed(world, ModItems.WOODEN_PIPE, DeviceType.PIPE);
        other.setStackInHand(Hand.MAIN_HAND, pipe);
        other.interactionManager.interactItem(other, world, pipe, Hand.MAIN_HAND);
        context.assertTrue(!other.isUsingItem(), "the pipe started a draw");
        context.assertEquals(charges(other), DeviceType.PIPE.bowlSize() - 1, "the pipe no longer hits on the click");
        context.complete();
    }

    private static int charges(ServerPlayerEntity player) {
        return player.getStackInHand(Hand.MAIN_HAND).getOrDefault(ModComponents.CHARGES, 0);
    }

    private static ItemStack packed(ServerWorld world, Item device, DeviceType type) {
        ItemStack stack = new ItemStack(device);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(world.getRegistryManager()
                .getOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA), 1));
        stack.set(ModComponents.CHARGES, type.bowlSize());
        return stack;
    }
}
