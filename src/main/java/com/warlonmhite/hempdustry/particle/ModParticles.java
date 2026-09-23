package com.warlonmhite.hempdustry.particle;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {

    /**
     * The smell of a ripe plant, drifting up off the top of it. Spawned by
     * {@code block/custom/RipeAroma} and drawn by {@code client/particle/AromaParticle}.
     *
     * <p>Not "always spawn", so vanilla's Particles setting governs it like any ambient particle:
     * Decreased drops a third of the wisps and Minimal drops them all.
     */
    public static final SimpleParticleType AROMA = registerParticle("aroma", FabricParticleTypes.simple());

    private static SimpleParticleType registerParticle(String name, SimpleParticleType type) {
        return Registry.register(Registries.PARTICLE_TYPE, Identifier.of(Hempdustry.MOD_ID, name), type);
    }

    public static void registerParticles() {
        Hempdustry.LOGGER.info("Registering Particles for " + Hempdustry.MOD_ID);
    }
}
