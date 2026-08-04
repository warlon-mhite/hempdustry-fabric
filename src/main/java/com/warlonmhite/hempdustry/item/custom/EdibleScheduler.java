package com.warlonmhite.hempdustry.item.custom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Applies an edible's effects late, and in sequence. Same shape as {@link SmokeScheduler} — one
 * server-thread queue, players looked up by UUID at fire time so the effect follows them and is
 * dropped if they have left or died.
 *
 * <p>Where the smoke scheduler waits 38 ticks for one particle puff, this one holds a whole staggered
 * bundle for anywhere from 30 seconds to three and a half minutes.
 *
 * <p><b>Known gap: this does not survive a logout.</b> An edible eaten and then disconnected on is
 * lost. That is a deliberate deferral rather than an oversight — the persistent version is a custom
 * "Digesting" status effect whose remaining duration <em>is</em> the clock, with its tick handler
 * firing each stage as the thresholds pass, which gets persistence for free because effects live on
 * the player entity. See CLAUDE.md §5b D13.
 */
public final class EdibleScheduler {
    private EdibleScheduler() {
    }

    private static final class Pending {
        final ServerWorld world;
        final UUID player;
        final StatusEffectInstance effect;
        int ticksLeft;

        Pending(ServerWorld world, UUID player, int ticksLeft, StatusEffectInstance effect) {
            this.world = world;
            this.player = player;
            this.ticksLeft = ticksLeft;
            this.effect = effect;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    /** Registers the tick pump. Call once from mod init. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    /** Applies {@code effect} to {@code player} in {@code delayTicks} ticks. */
    public static void schedule(ServerWorld world, PlayerEntity player, int delayTicks, StatusEffectInstance effect) {
        PENDING.add(new Pending(world, player.getUuid(), Math.max(1, delayTicks), effect));
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
                player.addStatusEffect(new StatusEffectInstance(pending.effect));
            }
            return true;
        });
    }
}
