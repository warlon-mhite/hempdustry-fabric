package com.warlonmhite.hempdustry.item.custom;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * How well a batch of cannabutter was made — the craftsmanship axis, entirely separate from how
 * <em>strong</em> it is. Strength is how much hemp went in; Quality is whether you did it properly.
 *
 * <h2>Two dials, one score</h2>
 *
 * Quality is a <b>continuous trade</b> between patience and prep, not a lookup table. Both axes are
 * measured out of 100 and simply added, giving a 0–200 extraction score:
 *
 * <pre>
 *   score = timePercent + washedPercent
 * </pre>
 *
 * <ul>
 *   <li><b>{@code timePercent}</b> — how far through the <em>collectable</em> window the batch is:
 *       0 at the early-pull minimum, 100 at a full simmer. (Before the minimum there is nothing to
 *       grade, so that is where the scale starts.)</li>
 *   <li><b>{@code washedPercent}</b> — what share of the hemp was washed. Integer-floored, so it
 *       only reads 100 when there is genuinely not one unwashed item in the batch.</li>
 * </ul>
 *
 * <h2>Why the weights are equal</h2>
 *
 * The two anchors of the design force it. An all-unwashed batch left the full time should land
 * exactly on {@link #STANDARD}, and an all-washed batch pulled at the earliest moment should land
 * exactly on {@link #STANDARD} too — patience and prep are worth the same, and <b>a full dose of
 * either one alone gets you to Standard</b>. Written out, those two requirements are
 * {@code a·100 = σ} and {@code b·100 = σ}, so {@code a = b}. There is no freedom left: any linear
 * score meeting both anchors weights the dials equally. Hence a plain sum, and hence
 * {@link #STANDARD_SCORE} {@code = 100}.
 *
 * <h2>Why 160 for Clean, and the coupling nobody expects</h2>
 *
 * {@link #CLEAN} is deliberately out of reach of a single dial — you cannot buy it with patience
 * alone or purity alone. With equal weights, though, two things you might want to tune separately
 * turn out to be <b>the same number</b>:
 *
 * <ul>
 *   <li>the lowest washed share that can ever reach Clean (at a full simmer) is
 *       {@code CLEAN_SCORE - 100};</li>
 *   <li>the time percentage at which an <em>all-washed</em> batch reaches Clean is also
 *       {@code CLEAN_SCORE - 100}.</li>
 * </ul>
 *
 * Both are {@code 60}. Pick one and you have picked the other: making Clean demand a greater
 * majority of washed hemp necessarily also makes a perfectly-prepped batch wait longer for it. 160
 * is the middle of that trade — <b>60% washed is the floor for Clean</b>, and a fully washed batch
 * earns Clean at {@code timePercent} 60, which is 73% of the total cook time.
 *
 * <h2>Perfect is a gate, not a threshold</h2>
 *
 * {@link #PERFECT} is checked before the score and needs <b>both dials maxed</b>: a full simmer and
 * not one unwashed item. It is not "score 200" because that would make it vulnerable to rounding;
 * it is the one grade the mechanic refuses to let you approximate. 99% washed at a full simmer is
 * Clean, and so is 100% washed one tick short.
 *
 * <p>The result is that {@link #ROUGH} stays a real choice rather than a punishment — the same shape
 * as vanilla letting you eat raw meat instead of cooked — and that a player can reach Clean well
 * before the full timer if they prepped properly, which is the whole point of washing hemp.
 */
public enum Quality implements StringIdentifiable {
    /** Pulled early, without the prep to make up for it. Under-extracted. */
    ROUGH("rough"),
    /** A full dose of one dial: all the patience, or all the prep. */
    STANDARD("standard"),
    /** Both dials well up. Needs a large majority of washed hemp and real patience. */
    CLEAN("clean"),
    /** Full simmer, exclusively washed hemp. Nothing cut anywhere. */
    PERFECT("perfect");

    /** Score at or above which a batch is {@link #STANDARD}. A full dose of either dial alone. */
    public static final int STANDARD_SCORE = 100;
    /**
     * Score at or above which a batch is {@link #CLEAN}. Out of reach of one dial by design; see the
     * class notes for why this single number sets both the washed floor and the all-washed timing.
     */
    public static final int CLEAN_SCORE = 160;

    public static final Codec<Quality> CODEC = StringIdentifiable.createCodec(Quality::values);
    /** Ordinal-based rather than string-based: this only ever travels over the wire, never to disk. */
    public static final PacketCodec<ByteBuf, Quality> PACKET_CODEC =
            PacketCodecs.VAR_INT.xmap(ordinal -> values()[ordinal], Quality::ordinal);

    private final String name;

    Quality(String name) {
        this.name = name;
    }

    /**
     * The grade a batch earns right now, from the two dials. Both are percentages clamped to 0–100;
     * see the class notes for the whole design.
     *
     * @param timePercent   0 at the early-pull minimum, 100 at a full simmer
     * @param washedPercent share of the batch's hemp that was washed, integer-floored so it only
     *                      reads 100 when nothing unwashed went in
     */
    public static Quality of(int timePercent, int washedPercent) {
        int time = MathHelper.clamp(timePercent, 0, 100);
        int washed = MathHelper.clamp(washedPercent, 0, 100);
        // Both dials maxed, checked before the score so Perfect can never be approximated into.
        if (time >= 100 && washed >= 100) {
            return PERFECT;
        }
        int score = time + washed;
        if (score >= CLEAN_SCORE) {
            return CLEAN;
        }
        return score >= STANDARD_SCORE ? STANDARD : ROUGH;
    }

    /**
     * The lowest {@code timePercent} at which a batch of this purity would earn {@code target}, or
     * {@code -1} if that grade is out of reach for it however long it simmers.
     *
     * <p>This is {@link #of} solved for time, and it exists so the GUI can show the player
     * <em>when</em> their batch will improve. Under the old all-or-nothing grading the answer was
     * always "the full timer"; now it moves with the washed ratio, so it has to be shown.
     */
    public static int timeNeededFor(Quality target, int washedPercent) {
        int washed = MathHelper.clamp(washedPercent, 0, 100);
        int needed = switch (target) {
            case ROUGH -> 0;
            case STANDARD -> STANDARD_SCORE - washed;
            case CLEAN -> CLEAN_SCORE - washed;
            // Perfect is a gate, not a score: nothing short of a spotless batch ever gets there.
            case PERFECT -> washed >= 100 ? 100 : Integer.MAX_VALUE;
        };
        if (needed > 100) {
            return -1;
        }
        return Math.max(0, needed);
    }

    /** The grade one step up from this one, or {@code null} for {@link #PERFECT}. */
    @Nullable
    public Quality next() {
        return this == PERFECT ? null : values()[this.ordinal() + 1];
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
