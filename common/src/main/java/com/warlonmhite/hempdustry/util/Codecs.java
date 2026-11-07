package com.warlonmhite.hempdustry.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Registry;

/**
 * Codecs the newer lines take from Minecraft and its DataFixerUpper, written out for 1.16.5, so the
 * mod's data files read and write in the same shape on every line. Minecraft 1.16.5 has no
 * {@code ExtraCodecs}, so its few members the mod uses are here too.
 */
public final class Codecs {

    private Codecs() {
    }

    /** Any int of 0 or more: the newer lines' {@code ExtraCodecs.NON_NEGATIVE_INT}. */
    public static final Codec<Integer> NON_NEGATIVE_INT = intAtLeast(0);

    /** Any int of 1 or more: the newer lines' {@code ExtraCodecs.POSITIVE_INT}. */
    public static final Codec<Integer> POSITIVE_INT = intAtLeast(1);

    /** Any float above 0: the newer lines' {@code ExtraCodecs.POSITIVE_FLOAT}. */
    public static final Codec<Float> POSITIVE_FLOAT = validate(Codec.FLOAT, value -> value > 0.0F
            ? DataResult.success(value)
            : DataResult.error("Value must be positive: " + value));

    private static Codec<Integer> intAtLeast(int min) {
        return validate(Codec.INT, value -> value >= min
                ? DataResult.success(value)
                : DataResult.error("Value must be " + (min == 0 ? "non-negative" : "positive") + ": " + value));
    }

    /** {@code codec}, refusing what {@code check} refuses both ways: the newer lines' {@code ExtraCodecs.validate}. */
    public static <A> Codec<A> validate(Codec<A> codec, Function<A, DataResult<A>> check) {
        return codec.flatXmap(check, check);
    }

    /** A list of {@code min} to {@code max} entries: newer DataFixerUpper's {@code listOf(min, max)}. */
    public static <E> Codec<List<E>> listOf(Codec<E> element, int min, int max) {
        return validate(element.listOf(), list -> list.size() < min || list.size() > max
                ? DataResult.error("expected " + (min == max ? String.valueOf(min) : min + " to " + max)
                        + " entries, got " + list.size())
                : DataResult.success(list));
    }

    /**
     * A status effect as Minecraft 1.20.5 and later write one — {@code id}, then {@code amplifier} and
     * {@code duration} (0 when absent), {@code ambient}, {@code show_particles} and {@code show_icon}.
     * 1.16.5 has no codec for it.
     */
    public static final Codec<MobEffectInstance> MOB_EFFECT_INSTANCE = RecordCodecBuilder.create(instance -> instance.group(
            Registry.MOB_EFFECT.fieldOf("id").forGetter(MobEffectInstance::getEffect),
            NON_NEGATIVE_INT.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance::getAmplifier),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(MobEffectInstance::getDuration),
            Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectInstance::isAmbient),
            Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance::isVisible),
            Codec.BOOL.optionalFieldOf("show_icon").forGetter(effect -> Optional.of(effect.showIcon()))
    ).apply(instance, (effect, amplifier, duration, ambient, particles, icon) ->
            new MobEffectInstance(effect, duration, amplifier, ambient, particles, icon.orElse(particles))));
}
