package com.warlonmhite.hempdustry.item.custom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

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
        /**
         * What to puff. Held rather than looked up on arrival because the stack that was smoked may
         * be gone by then — a spliff is consumed on the same tick it is used. Particle types are
         * registry singletons with no world or entity behind them, so parking one here for two
         * seconds keeps nothing else alive.
         */
        final ParticleEffect particle;
        int ticksLeft;

        Pending(UUID player, ParticleEffect particle, int ticksLeft) {
            this.player = player;
            this.particle = particle;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private static final class PendingSound {
        final UUID player;
        int ticksLeft;
        final SoundEvent sound;

        PendingSound(UUID player, int ticksLeft, SoundEvent sound) {
            this.player = player;
            this.ticksLeft = ticksLeft;
            this.sound = sound;
        }
    }

    private static final List<PendingSound> PENDING_SOUNDS = new ArrayList<>();

    /**
     * Status effects a strain holds back to the exhale. Built and filtered on the hit, so all this
     * does is wait: {@code EffectPolicy} has already had its say, and the instances are fresh.
     */
    private static final class PendingEffects {
        final UUID player;
        final List<StatusEffectInstance> effects;
        int ticksLeft;

        PendingEffects(UUID player, List<StatusEffectInstance> effects, int ticksLeft) {
            this.player = player;
            this.effects = effects;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final List<PendingEffects> PENDING_EFFECTS = new ArrayList<>();

    /** Registers the tick pump. Call once from mod init. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SmokeScheduler::tick);
        // Nothing drains this queue while no server is ticking, so an integrated server shut down
        // with entries still in it would strand them — and every entry keeps its player's UUID alive
        // in a static field for the rest of the process. Clearing on stop also means the next world
        // never inherits the last one's backlog.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING.clear();
            PENDING_SOUNDS.clear();
            PENDING_EFFECTS.clear();
        });
        // A player who dies inside the exhale window must not come back from the respawn screen
        // with the hit's effects: the respawn is a new, living entity with the same UUID, so the
        // isAlive check below cannot tell. Same fix as EdibleScheduler. `alive` is the End-portal
        // return, which is not a death.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                PENDING_EFFECTS.removeIf(pending -> pending.player.equals(newPlayer.getUuid()));
            }
        });
    }

    /** Schedules an exhale puff of {@code particle} for {@code player} in {@code delayTicks} ticks. */
    public static void schedule(PlayerEntity player, ParticleEffect particle, int delayTicks) {
        PENDING.add(new Pending(player.getUuid(), particle, Math.max(1, delayTicks)));
    }

    /** Applies {@code effects} to {@code player} in {@code delayTicks} ticks, if they are still alive. */
    public static void scheduleEffects(PlayerEntity player, List<StatusEffectInstance> effects, int delayTicks) {
        PENDING_EFFECTS.add(new PendingEffects(player.getUuid(), List.copyOf(effects), Math.max(1, delayTicks)));
    }

    /** Schedules {@code sound} to play at {@code player}'s position in {@code delayTicks} ticks. */
    public static void scheduleSound(PlayerEntity player, int delayTicks, SoundEvent sound) {
        PENDING_SOUNDS.add(new PendingSound(player.getUuid(), Math.max(1, delayTicks), sound));
    }

    private static void tick(MinecraftServer server) {
        if (!PENDING.isEmpty()) {
            PENDING.removeIf(pending -> {
                if (--pending.ticksLeft > 0) {
                    return false;
                }
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.player);
                if (player != null && player.isAlive()) {
                    Smoking.spawnExhale(player.getEntityWorld(), player, pending.particle);
                }
                return true;
            });
        }
        if (!PENDING_EFFECTS.isEmpty()) {
            PENDING_EFFECTS.removeIf(pending -> {
                if (--pending.ticksLeft > 0) {
                    return false;
                }
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.player);
                if (player != null && player.isAlive()) {
                    pending.effects.forEach(player::addStatusEffect);
                }
                return true;
            });
        }
        if (!PENDING_SOUNDS.isEmpty()) {
            PENDING_SOUNDS.removeIf(pending -> {
                if (--pending.ticksLeft > 0) {
                    return false;
                }
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.player);
                if (player != null && player.isAlive()) {
                    player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            pending.sound, SoundCategory.PLAYERS, 1f, 1f);
                }
                return true;
            });
        }
    }
}
