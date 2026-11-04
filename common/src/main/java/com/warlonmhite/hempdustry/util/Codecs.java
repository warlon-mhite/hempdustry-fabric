package com.warlonmhite.hempdustry.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Codecs the newer lines take from Minecraft and its DataFixerUpper, written out for 1.20.1, so the
 * mod's data files read and write in the same shape on every line.
 */
public final class Codecs {

    private Codecs() {
    }

    /** A list of {@code min} to {@code max} entries: newer DataFixerUpper's {@code listOf(min, max)}. */
    public static <E> Codec<List<E>> listOf(Codec<E> element, int min, int max) {
        return ExtraCodecs.validate(element.listOf(), list -> list.size() < min || list.size() > max
                ? DataResult.error(() -> "expected " + (min == max ? String.valueOf(min) : min + " to " + max)
                        + " entries, got " + list.size())
                : DataResult.success(list));
    }

    /**
     * A status effect as Minecraft 1.20.5 and later write one — {@code id}, then {@code amplifier} and
     * {@code duration} (0 when absent), {@code ambient}, {@code show_particles} and {@code show_icon}.
     * 1.20.1 has no codec for it.
     */
    public static final Codec<MobEffectInstance> MOB_EFFECT_INSTANCE = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("id").forGetter(MobEffectInstance::getEffect),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance::getAmplifier),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(MobEffectInstance::getDuration),
            Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectInstance::isAmbient),
            Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance::isVisible),
            Codec.BOOL.optionalFieldOf("show_icon").forGetter(effect -> Optional.of(effect.showIcon()))
    ).apply(instance, (effect, amplifier, duration, ambient, particles, icon) ->
            new MobEffectInstance(effect, duration, amplifier, ambient, particles, icon.orElse(particles))));
}
