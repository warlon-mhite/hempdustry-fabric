package com.warlonmhite.hempdustry.item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies an edible's effects late, and in sequence. Where {@link SmokeScheduler} waits 38 ticks for
 * one particle puff, this one holds a whole staggered bundle for anywhere from 45 seconds to nearly
 * five minutes -- a Rough onset of up to 4:20, and the 30-second ramp after it.
 *
 * <h2>The doses wait on the player, and are saved with them</h2>
 *
 * What is still to land is a list of {@link Pending} doses attached to the player
 * ({@code hempdustry:pending_edibles}), and written into the player's save with everything else. So
 * an edible eaten just before a logout lands after the login, and a dedicated server's restart voids
 * nothing. Over a wait this long a logout is ordinary, and until 2.1 it lost the edible. The clock
 * stops while the player is away, as a status effect's does.
 *
 * <p>A status effect was the other way to hold the clock, but a player carries one instance of each
 * effect: a second edible eaten during the first one's wait would replace it or hide behind it, where
 * here both land, as they always have. Milk and a totem, which clear every effect, do not touch an
 * edible still on its way down either.
 *
 * <p><b>Dying drops what you have eaten.</b> The attachment is not copied to the player who respawns
 * after a death; it is copied through the End portal's return, which is not one. A nether trip keeps
 * it too: it is the same player, wherever they are.
 */
public final class EdibleScheduler {
    private EdibleScheduler() {
    }

    /** One effect waiting to land, and how many ticks it has left. */
    public record Pending(int ticksLeft, StatusEffectInstance effect) {
        public static final Codec<Pending> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codecs.POSITIVE_INT.fieldOf("ticks_left").forGetter(Pending::ticksLeft),
                StatusEffectInstance.CODEC.fieldOf("effect").forGetter(Pending::effect)
        ).apply(instance, Pending::new));
    }

    /** Everything a player has eaten that has not landed yet, oldest first. Saved with the player. */
    public static final AttachmentType<List<Pending>> PENDING = AttachmentRegistry.create(
            Identifier.of(Hempdustry.MOD_ID, "pending_edibles"),
            builder -> builder.persistent(Pending.CODEC.listOf()));

    /** Registers the tick pump. Call once from mod init. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(EdibleScheduler::tick);
    }

    /** Applies {@code effect} to {@code player} in {@code delayTicks} ticks. */
    public static void schedule(PlayerEntity player, int delayTicks, StatusEffectInstance effect) {
        List<Pending> waiting = new ArrayList<>(player.getAttachedOrElse(PENDING, List.of()));
        waiting.add(new Pending(Math.max(1, delayTicks), new StatusEffectInstance(effect)));
        player.setAttached(PENDING, List.copyOf(waiting));
    }

    /** What {@code player} still has on its way, oldest first. */
    public static List<Pending> pending(PlayerEntity player) {
        return player.getAttachedOrElse(PENDING, List.of());
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            List<Pending> waiting = player.getAttached(PENDING);
            // A player lying dead waits for the respawn screen, which drops the list; nothing lands on a corpse.
            if (waiting == null || !player.isAlive()) {
                continue;
            }
            List<Pending> still = new ArrayList<>(waiting.size());
            for (Pending pending : waiting) {
                if (pending.ticksLeft() > 1) {
                    still.add(new Pending(pending.ticksLeft() - 1, pending.effect()));
                } else {
                    player.addStatusEffect(new StatusEffectInstance(pending.effect()));
                }
            }
            if (still.isEmpty()) {
                player.removeAttached(PENDING);
            } else {
                player.setAttached(PENDING, List.copyOf(still));
            }
        }
    }
}
