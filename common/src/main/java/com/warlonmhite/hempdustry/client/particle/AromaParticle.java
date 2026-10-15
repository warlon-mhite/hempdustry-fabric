package com.warlonmhite.hempdustry.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * One wisp of a ripe plant's smell: a faint curl that rises slowly off the flower, sways, spreads
 * and fades. It reads as a smell rather than smoke on purpose — the mod's smoke is the smoking
 * devices' exhale, and a field that seemed to smoulder would look like it was on fire.
 *
 * <p><b>Built to be cheap in the thousands.</b> A huge ripe field puts a few hundred of these in
 * the air around a player, so the per-tick work is kept to arithmetic:
 * <ul>
 *   <li>{@code hasPhysics} is off, which skips the world collision query every vanilla
 *       particle runs on every tick of its life. That is safe because {@code RipeAroma} only starts
 *       a wisp under open air, and a wisp never rises far enough to reach the block above that.</li>
 *   <li>No gravity and no drag: the rise is a constant and the sway a sine of the age, so nothing
 *       accumulates and nothing needs clamping.</li>
 * </ul>
 * Vanilla still bounds the total — every particle group is capped at 16384, the oldest evicted.
 */
public class AromaParticle extends TextureSheetParticle {
    private static final float MAX_ALPHA = 0.9f;
    private static final int FADE_IN_TICKS = 6;
    /** The last this fraction of a wisp's life is spent fading out. */
    private static final float FADE_OUT_FRACTION = 0.35f;
    /** How much wider a wisp is when it dies than when it starts: a smell spreads as it rises. */
    private static final float SPREAD = 0.6f;
    private static final double SWAY = 0.004;
    private static final float SWAY_SPEED = 0.15f;

    private final SpriteSet sprites;
    private final double driftX;
    private final double driftZ;
    private final float swayPhase;

    AromaParticle(ClientLevel world, double x, double y, double z, SpriteSet sprites) {
        super(world, x, y, z);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        // Rise 0.4–0.9 of a block over the whole life: from the top of the flower it never
        // reaches the block above the air it started in.
        this.lifetime = 40 + this.random.nextInt(21);
        this.yd = 0.010 + this.random.nextDouble() * 0.005;
        this.driftX = (this.random.nextDouble() - 0.5) * 0.004;
        this.driftZ = (this.random.nextDouble() - 0.5) * 0.004;
        this.swayPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.quadSize = 0.16f + this.random.nextFloat() * 0.06f;
        this.alpha = 0.0f;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        float sway = this.swayPhase + this.age * SWAY_SPEED;
        this.xd = this.driftX + SWAY * Mth.sin(sway);
        this.zd = this.driftZ + SWAY * Mth.cos(sway);
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            float fadeIn = (float) this.age / FADE_IN_TICKS;
            float fadeOut = (this.lifetime - this.age) / (FADE_OUT_FRACTION * this.lifetime);
            this.alpha = MAX_ALPHA * Mth.clamp(Math.min(fadeIn, fadeOut), 0.0f, 1.0f);
        }
    }

    @Override
    public float getQuadSize(float tickDelta) {
        return this.quadSize * (1.0f + SPREAD * (this.age + tickDelta) / this.lifetime);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new AromaParticle(world, x, y, z, this.sprites);
        }
    }
}
