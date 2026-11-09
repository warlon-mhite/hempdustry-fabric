package com.warlonmhite.hempdustry.particle;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public class ModParticles {

    public static final SimpleParticleType AROMA = registerParticle("aroma", new SimpleParticleType(false) {
    });

    /**
     * The three particles a terpene tints, each carrying its colour already mixed: the exhale of a
     * spliff, pipe or bong (vanilla's cosy campfire smoke, a third of the colour in its grey), the
     * vaporizer's (vanilla's cloud, a quarter in its white), and the ripe plant's aroma wisp (the
     * smoke's share). A load or a plant with no terpene keeps the plain particle.
     */
    public static final ParticleType<ColorParticleOption> TERPENE_SMOKE = registerColored("terpene_smoke");
    public static final ParticleType<ColorParticleOption> TERPENE_VAPOUR = registerColored("terpene_vapour");
    public static final ParticleType<ColorParticleOption> TERPENE_AROMA = registerColored("terpene_aroma");

    private static SimpleParticleType registerParticle(String name, SimpleParticleType type) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name), type);
    }

    private static ParticleType<ColorParticleOption> registerColored(String name) {
        ParticleType<ColorParticleOption> type = new ParticleType<>(false) {
            @Override
            public MapCodec<ColorParticleOption> codec() {
                return ColorParticleOption.codec(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                return ColorParticleOption.streamCodec(this);
            }
        };
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name), type);
    }

    public static void registerParticles() {
        Hempdustry.LOGGER.info("Registering Particles for " + Hempdustry.MOD_ID);
    }
}
