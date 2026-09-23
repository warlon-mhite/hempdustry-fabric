package com.warlonmhite.hempdustry.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;

/**
 * One wisp of a ripe plant's smell: a faint curl that rises slowly off the flower, sways, spreads
 * and fades. It reads as a smell rather than smoke on purpose — the mod's smoke is the smoking
 * devices' exhale, and a field that seemed to smoulder would look like it was on fire.
 *
 * <p><b>Built to be cheap in the thousands.</b> A huge ripe field puts a few hundred of these in
 * the air around a player, so the per-tick work is kept to arithmetic:
 * <ul>
 *   <li>{@code collidesWithWorld} is off, which skips the world collision query every vanilla
 *       particle runs on every tick of its life. That is safe because {@code RipeAroma} only starts
 *       a wisp under open air, and a wisp never rises far enough to reach the block above that.</li>
 *   <li>No gravity and no drag: the rise is a constant and the sway a sine of the age, so nothing
 *       accumulates and nothing needs clamping.</li>
 * </ul>
 * Vanilla still bounds the total — every particle group is capped at 16384, the oldest evicted.
 */
public class AromaParticle extends SpriteBillboardParticle {
    private static final float MAX_ALPHA = 0.9f;
    private static final int FADE_IN_TICKS = 6;
    /** The last this fraction of a wisp's life is spent fading out. */
    private static final float FADE_OUT_FRACTION = 0.35f;
    /** How much wider a wisp is when it dies than when it starts: a smell spreads as it rises. */
    private static final float SPREAD = 0.6f;
    private static final double SWAY = 0.004;
    private static final float SWAY_SPEED = 0.15f;

    private final SpriteProvider sprites;
    private final double driftX;
    private final double driftZ;
    private final float swayPhase;

    AromaParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
        super(world, x, y, z);
        this.sprites = sprites;
        this.collidesWithWorld = false;
        this.gravityStrength = 0.0f;
        this.velocityMultiplier = 1.0f;
        // Rise 0.4–0.9 of a block over the whole life: from the top of the flower it never
        // reaches the block above the air it started in.
        this.maxAge = 40 + this.random.nextInt(21);
        this.velocityY = 0.010 + this.random.nextDouble() * 0.005;
        this.driftX = (this.random.nextDouble() - 0.5) * 0.004;
        this.driftZ = (this.random.nextDouble() - 0.5) * 0.004;
        this.swayPhase = this.random.nextFloat() * MathHelper.TAU;
        this.scale = 0.16f + this.random.nextFloat() * 0.06f;
        this.alpha = 0.0f;
        this.setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        float sway = this.swayPhase + this.age * SWAY_SPEED;
        this.velocityX = this.driftX + SWAY * MathHelper.sin(sway);
        this.velocityZ = this.driftZ + SWAY * MathHelper.cos(sway);
        super.tick();
        if (!this.dead) {
            this.setSpriteForAge(this.sprites);
            float fadeIn = (float) this.age / FADE_IN_TICKS;
            float fadeOut = (this.maxAge - this.age) / (FADE_OUT_FRACTION * this.maxAge);
            this.alpha = MAX_ALPHA * MathHelper.clamp(Math.min(fadeIn, fadeOut), 0.0f, 1.0f);
        }
    }

    @Override
    public float getSize(float tickDelta) {
        return this.scale * (1.0f + SPREAD * (this.age + tickDelta) / this.maxAge);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new AromaParticle(world, x, y, z, this.sprites);
        }
    }
}
