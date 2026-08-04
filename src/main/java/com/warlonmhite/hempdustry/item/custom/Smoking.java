package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.advancement.ModCriteria;
import com.warlonmhite.hempdustry.sound.ModSounds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared "take a hit" feedback so the spliff, pipe and bong don't each re-implement it. Call from
 * the server side only. The strain-specific status effect will hook in here (or alongside it) once
 * the effects system lands.
 */
public final class Smoking {
    private Smoking() {
    }

    /** Ticks after the hit before the smoke puffs, to line up with the exhale in the sound (~1.5s). */
    private static final int EXHALE_DELAY_TICKS = 38;

    /**
     * A brief "greened out" wobble. Vanilla only ramps the nausea distortion up while the effect has
     * ≥60 ticks left (at ~1/150 per tick), then fades over the final 3s — so anything much shorter is
     * imperceptible. 140t ≈ 80t of build (peaks ~0.53 intensity) + the 3s fade: felt, but still short.
     */
    private static final int NAUSEA_DURATION_TICKS = 140; // 7s

    /** How long a green-out holds you down. Short on purpose — a setback, not a punishment. */
    private static final int GREEN_OUT_DURATION_TICKS = 300; // 15s
    /** The wobble outlasts the rest of it, so you feel it after you can move again. */
    private static final int GREEN_OUT_NAUSEA_TICKS = 400;   // 20s

    /**
     * Odds of greening out, as 1-in-N, indexed by dose. <b>Dose 1 can never green you out</b> — the
     * cheap everyday hit carries no tail risk at all, which is what makes taking a big one a real
     * decision rather than a free upgrade. A spliff halves these (see {@code greenOutChanceOneIn}):
     * you pace a joint, you don't pace a bong rip.
     */
    private static final int[] GREEN_OUT_ONE_IN = {0, 0, 12, 4};

    /** Green-out odds for {@code dose}, doubled (i.e. halved risk) when {@code gentle}. */
    public static int greenOutChanceOneIn(int dose, boolean gentle) {
        int index = Math.min(Math.max(dose, 0), GREEN_OUT_ONE_IN.length - 1);
        int base = GREEN_OUT_ONE_IN[index];
        return base == 0 ? 0 : (gentle ? base * 2 : base);
    }

    /**
     * The full server-side reaction to one hit: applies what is loaded at the dose that was packed,
     * for as long as this device lasts, plays the smoking sound, schedules the exhale puff, and rolls
     * the (separate) cough, nausea and green-out chances. Shared by the spliff, pipe and bong.
     *
     * <p>A green-out <b>replaces</b> the hit's effects rather than stacking on top of them. That is
     * what makes it a real loss and instantly readable — you spent three buds and got none of the
     * good part — instead of a debuff quietly layered under the buffs you were expecting.
     */
    public static void takeHit(World world, PlayerEntity player, SmokeContents contents,
                               int durationTicks, int coughChanceOneIn, int nauseaChanceOneIn,
                               int greenOutChanceOneIn) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SMOKING, SoundCategory.PLAYERS, 1f, 1f);

        boolean greenedOut = greenOutChanceOneIn > 0
                && ThreadLocalRandom.current().nextInt(greenOutChanceOneIn) == 0;

        if (greenedOut) {
            greenOut(player);
        } else {
            for (StatusEffectInstance effect : contents.effects(durationTicks)) {
                player.addStatusEffect(effect);
            }
        }

        // Smoke criterion: device- and strain-agnostic, fires on every hit. Backs "First Contact"
        // (any time) and "Blaze It!" (only inside the 4:20 window); the time gate lives in the
        // conditions, so we just hand it the current time of day (tick 0 = 6:00 AM).
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ModCriteria.SMOKE.trigger(serverPlayer, world.getTimeOfDay() % 24000L);
        }

        if (world instanceof ServerWorld serverWorld) {
            SmokeScheduler.schedule(serverWorld, player, EXHALE_DELAY_TICKS);
        }

        if (coughChanceOneIn > 0 && ThreadLocalRandom.current().nextInt(coughChanceOneIn) == 0) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.COUGHING, SoundCategory.PLAYERS, 1f, 1f);
        }

        // Nausea is its own roll and stays per-device, dose-independent — it is the "harsh smoke"
        // cost, not the "too much" cost. A green-out already brings its own, longer nausea.
        if (!greenedOut && nauseaChanceOneIn > 0
                && ThreadLocalRandom.current().nextInt(nauseaChanceOneIn) == 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, NAUSEA_DURATION_TICKS, 0));
        }
    }

    /** Sit down for a minute. Sweaty, wobbly, useless — but brief, and it costs you nothing but the buds. */
    private static void greenOut(PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, GREEN_OUT_NAUSEA_TICKS, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, GREEN_OUT_DURATION_TICKS, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, GREEN_OUT_DURATION_TICKS, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, GREEN_OUT_DURATION_TICKS, 1));
    }

    /** A small smoke puff at the player's mouth, drifting the way they're facing. */
    static void spawnExhale(ServerWorld world, PlayerEntity player) {
        Vec3d look = player.getRotationVector();
        double x = player.getX() + look.x * 0.5;
        double y = player.getEyeY() - 0.1 + look.y * 0.5;
        double z = player.getZ() + look.z * 0.5;
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 8, 0.02, 0.02, 0.02, 0.005);
    }
}
