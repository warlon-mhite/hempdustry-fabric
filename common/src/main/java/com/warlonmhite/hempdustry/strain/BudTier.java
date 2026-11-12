package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.component.ModComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * How well a harvest was grown, as one of four tiers above schwag: Mids, Dank, Top Shelf and Epic.
 *
 * <h2>One roll per harvest, in vanilla's loot-luck grammar</h2>
 *
 * Every bud of a harvest, Fortune's extras included, takes one tier drawn from its plant's grow luck
 * {@code L}: each tier weighs {@code max(⌊w + q × L⌋, 0)}, the formula vanilla's loot entries weigh a
 * pool by ({@code UniformContainerBase$EntryBase#getWeight}), with Mids 60 / −10, Dank 35 / +4, Top
 * Shelf 0 / +6 and Epic −6 / +2. So Mids fades as luck grows, Top Shelf appears from L 1 and Epic only
 * from L 4: genetics, care and setup together, never one act. A stressed or mildewed plant does not
 * roll: it gives Mids.
 *
 * <h2>What a tier buys when smoked</h2>
 *
 * Smoothness and length, never level: a tier scales how long its own entry's effects last
 * ({@link #durationFactor}) and the odds of the hit's cough, Nausea and green-out
 * ({@link #coughFactor}, {@link #nauseaFactor}, {@link #greenOutFactor}). Nothing in butter: the oven
 * boils off what a tier buys.
 *
 * <h2>Absent means Dank</h2>
 *
 * Every bud and every piece of hash 2.1 made reads Dank, and {@link #stamp} never writes Dank, so a
 * new Dank bud stacks with an old one. The items that carry a tier carry Dank on their prototype too,
 * so a stack naming it explicitly loses it. A tier's name colour is vanilla's {@code rarity}: Top
 * Shelf rare (aqua), Epic epic (light purple), Mids and Dank the item's own.
 */
public enum BudTier implements StringRepresentable {
    MIDS("mids", 60, -10, 1.5F, 0.75F, 1.0F, 1.0F, null),
    DANK("dank", 35, 4, 1.0F, 1.0F, 0.75F, 0.75F, null),
    TOP_SHELF("top_shelf", 0, 6, 0.5F, 1.25F, 0.5F, 0.5F, Rarity.RARE),
    EPIC("epic", -6, 2, 0.0F, 1.5F, 0.0F, 0.25F, Rarity.EPIC);

    public static final Codec<BudTier> CODEC = StringRepresentable.fromEnum(BudTier::values);
    /**
     * Ordinal-based: this only travels over the wire, never to disk. An id out of range (a bad
     * creative packet) reads Mids, the first tier, rather than throwing.
     */
    public static final StreamCodec<ByteBuf, BudTier> PACKET_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(BudTier::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), BudTier::ordinal);

    private final String name;
    private final int weight;
    private final int quality;
    private final float cough;
    private final float duration;
    private final float nausea;
    private final float greenOut;
    private final Rarity rarity;

    BudTier(String name, int weight, int quality, float cough, float duration, float nausea, float greenOut,
            Rarity rarity) {
        this.name = name;
        this.weight = weight;
        this.quality = quality;
        this.cough = cough;
        this.duration = duration;
        this.nausea = nausea;
        this.greenOut = greenOut;
        this.rarity = rarity;
    }

    /** This tier's weight at grow luck {@code luck}: vanilla's {@code max(⌊weight + quality × luck⌋, 0)}. */
    public int weightAt(float luck) {
        return Math.max(Mth.floor((float) weight + (float) quality * luck), 0);
    }

    /**
     * One harvest's tier at grow luck {@code luck}: exactly one draw on {@code random}, whatever it
     * decides, so the caller always knows how many it took.
     */
    public static BudTier roll(float luck, RandomSource random) {
        int total = 0;
        for (BudTier tier : values()) {
            total += tier.weightAt(luck);
        }
        // Mids gains weight as luck falls and Dank as it rises, so the total is never zero.
        int pick = random.nextInt(Math.max(1, total));
        for (BudTier tier : values()) {
            pick -= tier.weightAt(luck);
            if (pick < 0) {
                return tier;
            }
        }
        return MIDS;
    }

    /** How a tier ranks: Mids 0, Dank 1, Top Shelf 2, Epic 3. */
    public int rank() {
        return ordinal();
    }

    /** The tier of rank {@code rank}, held to the four. */
    public static BudTier ofRank(int rank) {
        return values()[Mth.clamp(rank, 0, values().length - 1)];
    }

    /** The weaker of {@code a} and {@code b}. */
    public static BudTier weaker(BudTier a, BudTier b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }

    /** What this tier multiplies the device's cough chance by: Mids half again, Epic none. */
    public float coughFactor() {
        return cough;
    }

    /** What this tier multiplies the device's duration by, for its own entry's effects. */
    public float durationFactor() {
        return duration;
    }

    /** What this tier multiplies the device's Nausea chance by: Mids as 2.1, Epic none. */
    public float nauseaFactor() {
        return nausea;
    }

    /** What this tier multiplies the green-out chance by: Mids as 2.1, Epic a quarter, never none. */
    public float greenOutFactor() {
        return greenOut;
    }

    /** {@code ticks} lasting as long as this tier makes them. */
    public int scaleDuration(int ticks) {
        return Math.round(ticks * duration);
    }

    /**
     * A one-in-{@code oneIn} chance multiplied by {@code chanceFactor}, as a one-in-N again: zero (never)
     * stays zero, and so does any chance multiplied by zero.
     */
    public static int scaleOneIn(int oneIn, float chanceFactor) {
        if (oneIn <= 0 || chanceFactor <= 0.0F) {
            return 0;
        }
        return Math.max(1, Math.round(oneIn / chanceFactor));
    }

    /** The tier an item carries, Dank when it carries none. */
    public static BudTier of(ItemStack stack) {
        return stack.getOrDefault(ModComponents.BUD_TIER, DANK);
    }

    /**
     * Writes {@code tier} on {@code stack} with its rarity; Dank writes nothing, and Mids and Dank
     * leave the name the item's own colour. Returns the stack. See {@link ModComponents#putDefaulted}.
     */
    public static ItemStack stamp(ItemStack stack, BudTier tier) {
        ModComponents.putDefaulted(stack, ModComponents.BUD_TIER, tier, DANK);
        return stampRarity(stack, tier);
    }

    /** Names {@code stack} in {@code tier}'s rarity: Top Shelf rare, Epic epic, Mids and Dank the item's own. */
    public static ItemStack stampRarity(ItemStack stack, BudTier tier) {
        Rarity own = stack.getPrototype().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        ModComponents.putDefaulted(stack, DataComponents.RARITY, tier.rarity == null ? own : tier.rarity, own);
        return stack;
    }

    /** Translation key of the line the player reads, e.g. {@code hempdustry.tier.top_shelf}. */
    public String getTranslationKey() {
        return "hempdustry.tier." + name;
    }

    /** The tooltip line, grey whatever the tier: the colour is the name's. */
    public Component tooltipLine() {
        return Component.translatable(getTranslationKey()).withStyle(ChatFormatting.GRAY);
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
