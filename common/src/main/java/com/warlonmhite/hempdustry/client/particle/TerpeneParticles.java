package com.warlonmhite.hempdustry.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.PlayerCloudParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/**
 * The exhale a terpene colours: vanilla's own cosy campfire smoke and cloud, made by vanilla's own
 * providers, then tinted with the colour the server sent. The colour is already mixed toward white
 * ({@code Smoking.tinted}), and a particle's colour multiplies its sprite, so grey smoke stays grey
 * with a cast of the terpene and never turns into paint.
 */
public final class TerpeneParticles {

    private TerpeneParticles() {
    }

    /** {@code hempdustry:terpene_smoke}: the spliff's, pipe's and bong's exhale. */
    public static final class Smoke implements ParticleProvider<ColorParticleOption> {
        private final CampfireSmokeParticle.CosyProvider vanilla;

        public Smoke(SpriteSet sprites) {
            this.vanilla = new CampfireSmokeParticle.CosyProvider(sprites);
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel world, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ, RandomSource random) {
            Particle particle = vanilla.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, world, x, y, z,
                    velocityX, velocityY, velocityZ, random);
            if (particle instanceof SingleQuadParticle quad) {
                quad.setColor(option.getRed(), option.getGreen(), option.getBlue());
            }
            return particle;
        }
    }

    /**
     * {@code hempdustry:terpene_vapour}: the vaporizer's. Vanilla shades each cloud puff a random
     * 70–100 % grey; the tint keeps that shading, so the vapour still billows.
     */
    public static final class Vapour implements ParticleProvider<ColorParticleOption> {
        private final PlayerCloudParticle.Provider vanilla;

        public Vapour(SpriteSet sprites) {
            this.vanilla = new PlayerCloudParticle.Provider(sprites);
        }

        @Override
        public Particle createParticle(ColorParticleOption option, ClientLevel world, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ, RandomSource random) {
            Particle particle = vanilla.createParticle(ParticleTypes.CLOUD, world, x, y, z,
                    velocityX, velocityY, velocityZ, random);
            if (particle instanceof SingleQuadParticle quad) {
                // Setting the colour replaces vanilla's random shade, so it is rolled again here.
                float shade = 1.0F - random.nextFloat() * 0.3F;
                quad.setColor(option.getRed() * shade, option.getGreen() * shade, option.getBlue() * shade);
            }
            return particle;
        }
    }
}
