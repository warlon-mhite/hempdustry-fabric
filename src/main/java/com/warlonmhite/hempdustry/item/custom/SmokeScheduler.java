package com.warlonmhite.hempdustry.item.custom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fires delayed exhale puffs so the smoke lines up with the exhale part of the smoking sound rather
 * than appearing the instant the player hits. Everything runs on the single server thread (item use
 * and the tick callback alike), so no synchronisation is needed.
 *
 * <p><b>The player is resolved off the ticking server, not off a stored world.</b> A queue entry
 * holds a UUID and nothing else: {@link net.minecraft.server.PlayerManager#getPlayer(UUID)} finds
 * them wherever they are, and the puff is spawned in whatever world they are standing in when it
 * fires. Holding a {@code ServerWorld} instead would have scoped the lookup to one dimension —
 * {@code ServerWorld.getPlayers()} is that dimension's list alone — so a player who stepped through
 * a portal in the meantime would silently get nothing. Same reason as
 * {@link EdibleScheduler}, where the window is minutes rather than two seconds and the loss is
 * correspondingly worse.
 */
public final class SmokeScheduler {
    private SmokeScheduler() {
    }

    private static final class Pending {
        final UUID player;
        int ticksLeft;

        Pending(UUID player, int ticksLeft) {
            this.player = player;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    /** Registers the tick pump. Call once from mod init. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SmokeScheduler::tick);
        // Nothing drains this queue while no server is ticking, so an integrated server shut down
        // with entries still in it would strand them — and every entry keeps its player's UUID alive
        // in a static field for the rest of the process. Clearing on stop also means the next world
        // never inherits the last one's backlog.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    /** Schedules an exhale puff for {@code player} in {@code delayTicks} ticks. */
    public static void schedule(PlayerEntity player, int delayTicks) {
        PENDING.add(new Pending(player.getUuid(), Math.max(1, delayTicks)));
    }

    private static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        PENDING.removeIf(pending -> {
            if (--pending.ticksLeft > 0) {
                return false;
            }
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.player);
            if (player != null && player.isAlive()) {
                Smoking.spawnExhale(player.getServerWorld(), player);
            }
            return true;
        });
    }
}
