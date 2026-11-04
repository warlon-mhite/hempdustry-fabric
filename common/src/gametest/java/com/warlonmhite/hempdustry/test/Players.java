package com.warlonmhite.hempdustry.test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.Connection;
import java.util.UUID;
import io.netty.channel.embedded.EmbeddedChannel;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
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
 * {@code PASS_TO_DEFAULT_BLOCK_INTERACTION}. So a test of an interaction clicks through the interaction
 * manager, the way these do, and a break a player causes goes through the player.
 */
final class Players {

    private Players() {
    }

    /**
     * Vanilla's mock player ({@code GameTestHelper#makeMockServerPlayerInLevel}), joined through a
     * connection with a channel under it. Vanilla's has none, and Forge's player list reads the
     * channel as the player joins; the channel here is a local one that keeps what is sent to it.
     * Answers CREATIVE to {@code isCreative()} whatever its game mode, as vanilla's does.
     */
    static ServerPlayer mock(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player);
        return player;
    }

    static ServerPlayer survival(GameTestHelper context) {
        ServerPlayer player = mock(context);
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
     * how a bucket a block turned down with {@code super} or {@code SKIP_DEFAULT_BLOCK_INTERACTION}
     * gets poured anyway. The player should face the block for the item's use to land where the
     * click did.
     */
    static InteractionResult clickLikeAClient(GameTestHelper context, ServerPlayer player, BlockPos pos, ItemStack held) {
        InteractionResult block = click(context, player, pos, held);
        if (block.consumesAction() || block == InteractionResult.FAIL) {
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
