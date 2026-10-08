package com.warlonmhite.hempdustry.test;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * A player for the game tests, and the clicks and breaks a real one makes.
 *
 * <p>{@code TestContext#useBlock} forgives what the game does not: it falls through to {@code onUse}
 * on any result a block did not accept, where the real interaction manager does so only after
 * {@code PASS_TO_DEFAULT_BLOCK_INTERACTION}. So a test of an interaction clicks through the interaction
 * manager, the way these do, and a break a player causes goes through the player.
 */
final class Players {

    private Players() {
    }

    static ServerPlayerEntity survival(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        return player;
    }

    /** The server's half of a right-click on the top of {@code pos}: what the interaction manager answers. */
    static ActionResult click(TestContext context, ServerPlayerEntity player, BlockPos pos, ItemStack held) {
        player.setStackInHand(Hand.MAIN_HAND, held);
        BlockPos at = context.getAbsolutePos(pos);
        return player.interactionManager.interactBlock(player, context.getWorld(), held, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(at), Direction.UP, at, false));
    }

    /**
     * A right-click the way the client plays it out ({@code MinecraftClient#doItemUse}): the block
     * first, and unless the block accepted it or failed it, the held item's own use after — which is
     * how a bucket a block turned down with {@code super} or {@code SKIP_DEFAULT_BLOCK_INTERACTION}
     * gets poured anyway. The player should face the block for the item's use to land where the
     * click did.
     */
    static ActionResult clickLikeAClient(TestContext context, ServerPlayerEntity player, BlockPos pos, ItemStack held) {
        ActionResult block = click(context, player, pos, held);
        if (block.isAccepted() || block == ActionResult.FAIL) {
            return block;
        }
        return player.interactionManager.interactItem(player, context.getWorld(), player.getStackInHand(Hand.MAIN_HAND),
                Hand.MAIN_HAND);
    }

    /** A break a player causes: the block's {@code onBreak}, the drops with the held tool, {@code afterBreak}, the stats. */
    static boolean breakAs(TestContext context, ServerPlayerEntity player, BlockPos pos) {
        return player.interactionManager.tryBreakBlock(context.getAbsolutePos(pos));
    }
}
