package com.warlonmhite.hempdustry.item.custom;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;

/**
 * How well a batch of cannabutter was made — the craftsmanship axis, entirely separate from how
 * <em>strong</em> it is. Strength is how much hemp went in; Quality is whether you did it properly.
 *
 * <p>Two things decide it, and they gate rather than merely improve:
 * <ul>
 *   <li><b>Patience.</b> Pulling a batch before it has finished simmering caps it at {@link #ROUGH}
 *       no matter what went in — the extraction genuinely didn't finish. Always available, never
 *       blocked; the same shape as vanilla letting you eat raw meat instead of cooked.</li>
 *   <li><b>Ingredient prep.</b> Washed hemp is what <em>permits</em> {@link #CLEAN} and
 *       {@link #PERFECT}. No amount of waiting turns an all-unwashed batch into anything better
 *       than {@link #STANDARD}.</li>
 * </ul>
 *
 * <p>So {@link #PERFECT} is deliberately the "did everything right" tier: full patience
 * <em>and</em> no shortcuts on prep. This mirrors brewing's Redstone-vs-Glowstone split, where two
 * independent dials produce a real trade rather than a strict upgrade.
 */
public enum Quality implements StringIdentifiable {
    /** Pulled early. Under-extracted, whatever the ingredients. */
    ROUGH("rough"),
    /** Full simmer, but nothing washed — chlorophyll and tannins came along for the ride. */
    STANDARD("standard"),
    /** Full simmer with some washed hemp in the mix. */
    CLEAN("clean"),
    /** Full simmer, exclusively washed hemp. Nothing cut anywhere. */
    PERFECT("perfect");

    public static final Codec<Quality> CODEC = StringIdentifiable.createCodec(Quality::values);
    /** Ordinal-based rather than string-based: this only ever travels over the wire, never to disk. */
    public static final PacketCodec<ByteBuf, Quality> PACKET_CODEC =
            PacketCodecs.VAR_INT.xmap(ordinal -> values()[ordinal], Quality::ordinal);

    private final String name;

    Quality(String name) {
        this.name = name;
    }

    /**
     * The grade a finished batch earns. {@code washed}/{@code unwashed} are item counts, not stacks.
     *
     * @param fullySimmered whether the batch ran the whole {@code FULL_TIME}, rather than being
     *                      pulled at the early minimum
     */
    public static Quality of(boolean fullySimmered, int washed, int unwashed) {
        if (!fullySimmered) {
            return ROUGH;
        }
        if (washed <= 0) {
            return STANDARD;
        }
        return unwashed > 0 ? CLEAN : PERFECT;
    }

    /** Translation key for the tooltip line, e.g. {@code hempdustry.quality.perfect}. */
    public String getTranslationKey() {
        return "hempdustry.quality." + this.name;
    }

    @Override
    public String asString() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.asString();
    }
}
