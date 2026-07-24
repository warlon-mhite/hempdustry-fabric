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

    /**
     * The full server-side reaction to one hit: applies the strain's effects at this device's
     * {@link Potency}, plays the smoking sound, schedules the exhale puff, and rolls the (separate)
     * cough sound and nausea chances. Shared by the spliff, pipe and bong.
     */
    public static void takeHit(World world, PlayerEntity player, Strain strain, Potency potency,
                               int coughChanceOneIn, int nauseaChanceOneIn) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SMOKING, SoundCategory.PLAYERS, 1f, 1f);

        for (StatusEffectInstance effect : strain.effects(potency)) {
            player.addStatusEffect(effect);
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

        if (nauseaChanceOneIn > 0 && ThreadLocalRandom.current().nextInt(nauseaChanceOneIn) == 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, NAUSEA_DURATION_TICKS, 0));
        }
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
