package com.warlonmhite.hempdustry.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A player for the game tests, and the clicks and breaks a real one makes.
 *
 * <p>{@code GameTestHelper#useBlock} forgives what the game does not: it falls through to {@code useWithoutItem}
 * on any result a block did not accept, where the real interaction manager does so only after
 * {@code TRY_WITH_EMPTY_HAND}. So a test of an interaction clicks through the interaction
 * manager, the way these do, and a break a player causes goes through the player.
 */
final class Players {

    private Players() {
    }

    /**
     * A survival player joined to the test's level, the way {@code GameTestHelper#makeMockServerPlayerInLevel}
     * joins one — but that one answers {@code gameMode()} with CREATIVE whatever its game mode is, so
     * every {@code isCreative()} check in the mod took the creative branch under test; and 26.2 marks it
     * for removal. This one's game mode is its own.
     */
    static ServerPlayer survival(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** The server's half of a right-click on the top of {@code pos}: what the interaction manager answers. */
    static InteractionResult click(GameTestHelper context, ServerPlayer player, BlockPos pos, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos at = context.absolutePos(pos);
        return player.gameMode.useItemOn(player, context.getLevel(), held, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
    }

    /**
     * A right-click the way the client plays it out ({@code Minecraft#startUseItem}): the block
     * first, and unless the block accepted it or failed it, the held item's own use after — which is
     * how a bucket a block turned down with {@code super} or {@code PASS} gets poured anyway. The
     * player should face the block for the item's use to land where the click did.
     */
    static InteractionResult clickLikeAClient(GameTestHelper context, ServerPlayer player, BlockPos pos, ItemStack held) {
        InteractionResult block = click(context, player, pos, held);
        if (block.consumesAction() || block instanceof InteractionResult.Fail) {
            return block;
        }
        return player.gameMode.useItem(player, context.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
    }

    /** A break a player causes: the block's {@code playerWillDestroy}, the drops with the held tool, {@code playerDestroy}, the stats. */
    static boolean breakAs(GameTestHelper context, ServerPlayer player, BlockPos pos) {
        return player.gameMode.destroyBlock(context.absolutePos(pos));
    }
}
