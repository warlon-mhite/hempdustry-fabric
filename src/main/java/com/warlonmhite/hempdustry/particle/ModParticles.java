package com.warlonmhite.hempdustry.particle;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ModParticles {

    /**
     * The smell of a ripe plant, drifting up off the top of it. Spawned by
     * {@code block/custom/RipeAroma} and drawn by {@code client/particle/AromaParticle}.
     *
     * <p>Not "always spawn", so vanilla's Particles setting governs it like any ambient particle:
     * Decreased drops a third of the wisps and Minimal drops them all. Vanilla's constructor is
     * protected, so the type is an empty subclass, as every mod's simple particle is.
     */
    public static final SimpleParticleType AROMA = registerParticle("aroma", new SimpleParticleType(false) {
    });

    private static SimpleParticleType registerParticle(String name, SimpleParticleType type) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, name), type);
    }

    public static void registerParticles() {
        Hempdustry.LOGGER.info("Registering Particles for " + Hempdustry.MOD_ID);
    }
}
