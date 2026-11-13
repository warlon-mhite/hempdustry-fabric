package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.util.ModTags;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * How much resin one plant makes, as one of five steps rolled at planting from its strain's odds
 * ({@link ResinSheet}): more kief at the Sifting Box and more charas at the rub, <b>never a stronger hash</b>.
 * A property of the strain, never of the place a plant grows. The plant hands its step to its buds, and the
 * rub reads it off the living plant.
 *
 * <table>
 *   <tr><th>step</th><th>sieve, full bonus</th><th>rub, full bonus</th></tr>
 *   <tr><td>very low</td><td>×0.5</td><td>1 in 8</td></tr>
 *   <tr><td>low</td><td>×0.75</td><td>1 in 6</td></tr>
 *   <tr><td>average</td><td>×1</td><td>1 in 4</td></tr>
 *   <tr><td>rich</td><td>×1.5</td><td>1 in 3</td></tr>
 *   <tr><td>very rich</td><td>×2, the ceiling</td><td>1 in 2</td></tr>
 * </table>
 *
 * <p>A low step costs in full on both routes; a rich step's bonus goes where its strain's {@link ResinTexture}
 * sends it. A lamp record or a stressed plant still halves the rub.
 *
 * <h2>Absent means what a bud did before resin</h2>
 *
 * A bud with no step sieves exactly as it always did: one level, or two for a bud in
 * {@code #hempdustry:siftable/resinous} (Beldía, grown for the sieve), which is a very rich dry plant's rate.
 * So that is the step such a bud reads ({@link #legacy}), and {@link #stamp} writes nothing for it: a new
 * average bud still stacks with an old one, and so does a new very rich Beldía bud.
 */
public enum ResinStep implements StringRepresentable {
    VERY_LOW("very_low", -2, 0.5F, 1.0F / 8),
    LOW("low", -1, 0.75F, 1.0F / 6),
    AVERAGE("average", 0, 1.0F, 1.0F / 4),
    RICH("rich", 1, 1.5F, 1.0F / 3),
    VERY_RICH("very_rich", 2, 2.0F, 1.0F / 2);

    public static final Codec<ResinStep> CODEC = StringRepresentable.fromEnum(ResinStep::values);
    /**
     * Ordinal-based: this only travels over the wire, never to disk. An id out of range (a bad creative packet)
     * reads very low, the first step, rather than throwing.
     */
    public static final StreamCodec<ByteBuf, ResinStep> PACKET_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(ResinStep::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), ResinStep::ordinal);

    private final String name;
    private final int offset;
    private final float sieve;
    private final float rub;

    ResinStep(String name, int offset, float sieve, float rub) {
        this.name = name;
        this.offset = offset;
        this.sieve = sieve;
        this.rub = rub;
    }

    /** This step's place around average: very low −2 to very rich +2. What a Stash Jar sums. */
    public int offset() {
        return offset;
    }

    /** The step {@code offset} places around average, held to the five. */
    public static ResinStep ofOffset(int offset) {
        return values()[Math.max(0, Math.min(values().length - 1, offset + 2))];
    }

    /** What a bud of this step multiplies its rate at the Sifting Box by, for a strain of {@code texture}. */
    public float sieveFactor(ResinTexture texture) {
        return offset <= 0 ? sieve : 1.0F + (sieve - 1.0F) * texture.sieveShare();
    }

    /** The chance that rubbing a plant of this step pays a charas, for a strain of {@code texture}, before a lamp halves it. */
    public float rubChance(ResinTexture texture) {
        float average = AVERAGE.rub;
        return offset <= 0 ? rub : average + (rub - average) * texture.rubShare();
    }

    /** The step an item carries, if one is written on it. */
    public static Optional<ResinStep> written(ItemStack stack) {
        return Optional.ofNullable(stack.get(ModComponents.RESIN_STEP));
    }

    /** The step a bud reads: the one written on it, else {@link #legacy}. */
    public static ResinStep of(ItemStack stack) {
        return written(stack).orElseGet(() -> legacy(stack));
    }

    /**
     * The step a bud with none written on it sieves as: very rich for a bud in {@code #hempdustry:siftable/resinous}
     * (two levels a bud), average for every other (one).
     */
    public static ResinStep legacy(ItemStack stack) {
        return stack.is(ModTags.Items.SIFTABLE_RESINOUS) ? VERY_RICH : AVERAGE;
    }

    /** Writes {@code step} on {@code stack}; the step it reads with none ({@link #legacy}) writes nothing. Returns the stack. */
    public static ItemStack stamp(ItemStack stack, ResinStep step) {
        if (step == legacy(stack)) {
            stack.remove(ModComponents.RESIN_STEP);
        } else {
            stack.set(ModComponents.RESIN_STEP, step);
        }
        return stack;
    }

    /** Translation key of the line the player reads, e.g. {@code hempdustry.resin.very_rich}. */
    public String getTranslationKey() {
        return "hempdustry.resin." + name;
    }

    /** The tooltip line, grey; average has none. */
    public Optional<Component> tooltipLine() {
        return this == AVERAGE
                ? Optional.empty()
                : Optional.of(Component.translatable(getTranslationKey()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
