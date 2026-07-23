package com.warlonmhite.hempdustry.item.custom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fires delayed exhale puffs so the smoke lines up with the exhale part of the smoking sound rather
 * than appearing the instant the player hits. Everything runs on the single server thread (item use
 * and the tick callback alike), so no synchronisation is needed. The player is looked up by UUID at
 * spawn time, so the puff tracks where they've moved to (and is skipped if they've left/died).
 */
public final class SmokeScheduler {
    private SmokeScheduler() {
    }

    private static final class Pending {
        final ServerWorld world;
        final UUID player;
        int ticksLeft;

        Pending(ServerWorld world, UUID player, int ticksLeft) {
            this.world = world;
            this.player = player;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    /** Registers the tick pump. Call once from mod init. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    /** Schedules an exhale puff for {@code player} in {@code delayTicks} ticks. */
    public static void schedule(ServerWorld world, PlayerEntity player, int delayTicks) {
        PENDING.add(new Pending(world, player.getUuid(), delayTicks));
    }

    private static void tick() {
        if (PENDING.isEmpty()) {
            return;
        }
        PENDING.removeIf(pending -> {
            if (--pending.ticksLeft > 0) {
                return false;
            }
            PlayerEntity player = pending.world.getPlayerByUuid(pending.player);
            if (player != null && player.isAlive()) {
                Smoking.spawnExhale(pending.world, player);
            }
            return true;
        });
    }
}
