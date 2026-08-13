package com.warlonmhite.hempdustry.item.custom;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Applies an edible's effects late, and in sequence. Same shape as {@link SmokeScheduler} — one
 * server-thread queue, players resolved by UUID at fire time. Where the smoke scheduler waits 38
 * ticks for one particle puff, this one holds a whole staggered bundle for anywhere from 30 seconds
 * to three and a half minutes.
 *
 * <h2>The player is found on the server, not in a world</h2>
 *
 * <b>A queue entry stores a UUID and nothing else.</b> The obvious shape — holding the
 * {@code ServerWorld} the edible was eaten in and calling {@code world.getPlayerByUuid} — scopes the
 * lookup to <em>one dimension</em>, because {@code ServerWorld.getPlayers()} is that dimension's list
 * alone. Over a window this long a nether trip is entirely ordinary, and the failure is silent: the
 * player eats a Space Cake, walks through a portal, and nothing ever happens. Resolving through
 * {@link net.minecraft.server.PlayerManager#getPlayer(UUID)} instead finds them wherever they are.
 *
 * <p>Not holding a world is also what stops this static queue from pinning a whole
 * {@code MinecraftServer} in memory — see {@link #init()}.
 *
 * <p><b>Known gap: this does not survive a logout.</b> An edible eaten and then disconnected on is
 * lost, and on a dedicated server a restart voids every queued bundle at once. That is a deliberate
 * deferral rather than an oversight — the persistent version is a custom "Digesting" status effect
 * whose remaining duration <em>is</em> the clock, with its tick handler firing each stage as the
 * thresholds pass, which gets persistence for free because effects live on the player entity.
 * See CLAUDE.md §5b D13.
 */
public final class EdibleScheduler {
    private EdibleScheduler() {
    }

    private static final class Pending {
        final UUID player;
        final StatusEffectInstance effect;
        int ticksLeft;

        Pending(UUID player, int ticksLeft, StatusEffectInstance effect) {
            this.player = player;
            this.ticksLeft = ticksLeft;
            this.effect = effect;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    /**
     * Registers the tick pump and the two things that empty the queue. Call once from mod init.
     *
     * <p><b>Clearing on server stop is not housekeeping.</b> Nothing drains this queue while no
     * server is ticking, so an integrated server quit to the title screen with entries still pending
     * leaves them there for the rest of the process — and a stale entry would then fire against
     * whatever world is opened next. Clearing on stop bounds the queue to one server's lifetime.
     *
     * <p><b>Dying drops what you have eaten</b>, which the {@code isAlive} check alone does not
     * achieve: a respawn hands back a <em>new</em> player entity with the same UUID, very much alive,
     * so a bundle queued before death would otherwise land on the fresh spawn. Purging on respawn is
     * what makes "you lose it if you die" true.
     */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(EdibleScheduler::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // `alive` is true for the End-portal return, which is not a death and keeps the bundle.
            if (!alive) {
                PENDING.removeIf(pending -> pending.player.equals(newPlayer.getUuid()));
            }
        });
    }

    /** Applies {@code effect} to {@code player} in {@code delayTicks} ticks. */
    public static void schedule(PlayerEntity player, int delayTicks, StatusEffectInstance effect) {
        PENDING.add(new Pending(player.getUuid(), Math.max(1, delayTicks), effect));
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
                player.addStatusEffect(new StatusEffectInstance(pending.effect));
            }
            return true;
        });
    }
}
