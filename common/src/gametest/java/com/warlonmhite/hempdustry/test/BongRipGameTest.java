package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.GameType;

/**
 * A bong is drawn, not clicked: the hit lands at the end of the hold, and letting go spends nothing.
 *
 * <p>Two quiet failures. A draw that hit on the click as well as at the end would be two hits for
 * one — the generous direction nobody reports. And a release that still spent a charge would make
 * the cancel a trap that looks like lag. The pipe is the control: it must still hit on the click,
 * or the draw has leaked into every device.
 */
public final class BongRipGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void bongIsDrawnNotClicked(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        ItemStack bong = packed(world, ModItems.BONG, DeviceType.BONG);
        player.setItemInHand(InteractionHand.MAIN_HAND, bong);
        player.gameMode.useItem(player, world, bong, InteractionHand.MAIN_HAND);
        context.assertTrue(player.isUsingItem(), "clicking a packed bong did not start a draw");
        context.assertTrue(player.getUseItem().getUseAnimation() == UseAnim.TOOT_HORN,
                "a bong draw is not raised to the mouth");
        context.assertValueEqual(DeviceType.BONG.builtInBowl().hits(), charges(player), "the click alone took a hit");

        player.releaseUsingItem();
        context.assertValueEqual(DeviceType.BONG.builtInBowl().hits(), charges(player), "letting go early spent a charge");
        context.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() == 0,
                "letting go early cost durability");

        player.gameMode.useItem(player, world, player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
        // What LivingEntity#consumeItem does at the end of a hold: finish, then let go. Skipping the
        // second leaves the player "using", and the pipe below would then prove nothing.
        ItemStack after = player.getUseItem().finishUsingItem(world, player);
        player.stopUsingItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, after);
        context.assertValueEqual(DeviceType.BONG.builtInBowl().hits() - 1, charges(player), "a finished draw did not take one hit");
        context.assertTrue(player.getCooldowns().isOnCooldown(after.getItem()),
                "a finished draw started no cooldown");

        // The control: the pipe still hits on the click, and never enters a draw. A fresh player,
        // because the cooldown the bong just started covers every smokeable, the pipe included.
        ServerPlayer other = context.makeMockServerPlayerInLevel();
        other.setGameMode(GameType.SURVIVAL);
        ItemStack pipe = packed(world, ModItems.WOODEN_PIPE, DeviceType.PIPE);
        other.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        other.gameMode.useItem(other, world, pipe, InteractionHand.MAIN_HAND);
        context.assertTrue(!other.isUsingItem(), "the pipe started a draw");
        context.assertValueEqual(DeviceType.PIPE.builtInBowl().hits() - 1, charges(other), "the pipe no longer hits on the click");
        context.succeed();
    }

    private static int charges(ServerPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).getOrDefault(ModComponents.CHARGES, 0);
    }

    private static ItemStack packed(ServerLevel world, Item device, DeviceType type) {
        ItemStack stack = new ItemStack(device);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(world.registryAccess()
                .lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA), 1));
        stack.set(ModComponents.CHARGES, type.builtInBowl().hits());
        return stack;
    }
}
