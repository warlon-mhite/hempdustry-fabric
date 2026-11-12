package com.warlonmhite.hempdustry.item.custom;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Potency;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * What is loaded in a spliff, pipe or bong — the payload of the {@code hempdustry:smoke_contents}
 * data component.
 *
 * <p>This is the mod's answer to {@code minecraft:potion_contents}: <b>one item per device</b> with
 * the strain carried as data, rather than a separate registered item per device × strain.
 * Empty contents means an unpacked device.
 *
 * <h2>Why this is a list when nothing mixes yet</h2>
 *
 * It will always hold exactly one entry until strain mixing lands. It is a list anyway because
 * <b>changing a component's codec after release is a world migration</b>, and a list costs nothing
 * now. {@code minecraft:firework_explosion}'s star list is the same shape for the same reason.
 *
 * <p>That argument only got the list half right, though: a bare top-level array is itself a shape
 * that cannot grow, because there is nowhere to hang a field belonging to the load rather than to one
 * strain within it. So the list now sits inside an object under {@code entries} — see
 * {@link #CODEC}, which still reads the old bare-array form so nothing already written is lost.
 *
 * <p>Until mixing is designed properly, a multi-entry blend resolves the obvious way: <b>each
 * strain applies its own effects at its own bud count</b>, so a 2+1 mix is one strain at level II
 * and another at level I. That falls out as a broader-but-shorter trade against a single strain at
 * dose three, which is roughly the fixed-budget shape mixing wants anyway — but it has not been
 * balanced, so treat it as a sane default rather than a design.
 */
public record SmokeContents(List<Entry> entries) {

    public static final SmokeContents EMPTY = new SmokeContents(List.of());

    /**
     * The entry list, <b>tolerant of an entry naming a strain this world no longer defines</b>.
     *
     * <h2>Why tolerance is not optional here</h2>
     *
     * {@link Strain#ENTRY_CODEC} is a {@code RegistryFixedCodec}, which fails when the id does not
     * resolve. A component whose codec fails takes the <em>whole {@code ItemStack}</em> down with it:
     * {@code DataComponentPatch.CODEC} is a {@code dispatchedMap}, so one bad value errors the map,
     * {@code ItemStack}'s codec errors in turn, and {@code ContainerHelper.loadAllItems} quietly drops the
     * slot. <b>The item is deleted.</b>
     *
     * <p>Which would mean that removing a strain from a datapack — or uninstalling an addon that
     * added one — silently destroys every spliff, pipe and bong packed with it, anywhere in the
     * world. For a mod whose headline extension point is "strains are datapack data", that is not an
     * acceptable failure mode.
     *
     * <p>So an entry that will not resolve is <b>dropped</b> instead. Pairing the real codec with
     * {@link Codec#PASSTHROUGH} — which accepts anything — means the element decode can never fail;
     * the unresolved ones come back as {@code Right} and are filtered out. A device simply becomes
     * unpacked and the player keeps it. The one wrinkle is the spliff, which has no meaningful empty
     * state: it becomes an inert spliff rather than vanishing, which is still strictly better than
     * losing the item, and it is at least visible and named plainly rather than silent.
     *
     * <p>This is also what finally makes {@link Strain#effects} honest — it documents a fallback for
     * "a strain the world no longer defines", which until now was unreachable because the stack had
     * already been deleted before anything could consult it.
     */
    private static final Codec<List<Entry>> ENTRIES_CODEC =
            Codec.either(Entry.CODEC, Codec.PASSTHROUGH).listOf().xmap(
                    SmokeContents::keepResolved,
                    entries -> entries.stream().map(Either::<Entry, Dynamic<?>>left).toList());

    /**
     * The record form, and <b>the shape written from now on</b>.
     *
     * <p>A bare list leaves nowhere to put a field that belongs to the load as a whole rather than to
     * one strain in it — resin, a burn timer, who rolled it. Adding one later would move the on-disk
     * shape from array to object and every packed device already in a chest would fail to decode.
     * Since a component's codec cannot be migrated without a world migration, the object form is
     * adopted now, while it costs a few bytes and nothing else.
     */
    private static final Codec<SmokeContents> RECORD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC.optionalFieldOf("entries", List.of()).forGetter(SmokeContents::entries)
    ).apply(instance, SmokeContents::new));

    /**
     * Writes the record form; reads either it or the original bare list.
     *
     * <p>{@code Codec.withAlternative} encodes through the primary and, on decode, tries the primary
     * and falls back to the alternative — so every stack written by a pre-2.0.0 development build
     * still loads, for ever, and no migration step is needed. The alternative costs one line and can
     * never be removed without breaking those stacks, which is the whole point of adding it before
     * release rather than after.
     */
    public static final Codec<SmokeContents> CODEC = Codec.withAlternative(
            RECORD_CODEC, ENTRIES_CODEC.xmap(SmokeContents::new, SmokeContents::entries));

    /**
     * Keeps the entries that resolved and complains once about any that did not.
     *
     * <p>Logged rather than silent because a strain vanishing from a world is a datapack problem
     * somebody needs to hear about — and because a total loss here would otherwise be indistinguishable
     * from decoding without a registry-aware {@code DynamicOps}, which drops everything for a quite
     * different reason.
     */
    private static List<Entry> keepResolved(List<Either<Entry, Dynamic<?>>> decoded) {
        List<Entry> kept = new ArrayList<>(decoded.size());
        for (Either<Entry, Dynamic<?>> entry : decoded) {
            entry.ifLeft(kept::add);
        }
        if (kept.size() < decoded.size()) {
            Hempdustry.LOGGER.warn(
                    "Dropped {} of {} smoke_contents entries naming a strain this world does not define. "
                            + "The item survives as unpacked; the strain was probably removed from a datapack.",
                    decoded.size() - kept.size(), decoded.size());
        }
        return kept;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SmokeContents> PACKET_CODEC =
            Entry.PACKET_CODEC.apply(ByteBufCodecs.list())
                    .map(SmokeContents::new, SmokeContents::entries);

    /**
     * A single strain's share of the load.
     *
     * <p>The strain is held as a {@link Holder} rather than by value: a stack stores the id
     * and resolves it against the world's strain registry, so a datapack edit reaches every spliff
     * already in a chest. {@link Strain#ENTRY_CODEC} refuses inline entries, which is what stops a
     * stack from carrying a stale copy of a definition.
     */
    public record Entry(Holder<Strain> strain, int count, Chemotype stamp, int potency, BudTier tier) {
        /**
         * {@code chemotype} is optional and left out when THC-dominant, and {@code potency} when 0, so
         * every entry 2.1 wrote reads as what it was, and a THC-dominant load of mid buds written now is
         * byte for byte what 2.1 would write.
         *
         * <p>{@code potency} is the sum of the band offsets ({@link PotencyBand#offset}) of a plant's buds
         * in the entry: a high bud and a mid one are +1, two low ones -2. Buds of different bands load
         * together, and the sum is exactly what their THC averages to, so nothing is lost by mixing.
         *
         * <p>{@code tier} is the entry's own {@link BudTier}, left out when Dank: a plant's buds the weakest
         * of them, a piece of sifted hash its batch's. It moves only this entry's effects.
         */
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Strain.ENTRY_CODEC.fieldOf("strain").forGetter(Entry::strain),
                Codec.INT.fieldOf("count").forGetter(Entry::count),
                Chemotype.CODEC.optionalFieldOf("chemotype", Chemotype.THC_DOMINANT).forGetter(Entry::stamp),
                Codec.INT.optionalFieldOf("potency", 0).forGetter(Entry::potency),
                BudTier.CODEC.optionalFieldOf("tier", BudTier.DANK).forGetter(Entry::tier)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> PACKET_CODEC = StreamCodec.composite(
                Strain.ENTRY_PACKET_CODEC, Entry::strain,
                ByteBufCodecs.VAR_INT, Entry::count,
                Chemotype.PACKET_CODEC, Entry::stamp,
                ByteBufCodecs.VAR_INT, Entry::potency,
                BudTier.PACKET_CODEC, Entry::tier,
                Entry::new);

        /** An entry with no stamp: a plant's buds, or anything 2.1 packed. */
        public Entry(Holder<Strain> strain, int count) {
            this(strain, count, Chemotype.THC_DOMINANT);
        }

        /** An entry of mid buds, or of anything that is not a plant's buds. */
        public Entry(Holder<Strain> strain, int count, Chemotype stamp) {
            this(strain, count, stamp, 0);
        }

        /** An entry of Dank matter: the shape every entry had before tiers. */
        public Entry(Holder<Strain> strain, int count, Chemotype stamp, int potency) {
            this(strain, count, stamp, potency, BudTier.DANK);
        }

        /** This entry at {@code tier}. */
        public Entry withTier(BudTier tier) {
            return new Entry(strain, count, stamp, potency, tier);
        }

        /**
         * Whether this entry smokes outside the tiers: schwag, spoiled bud, which is the bottom of the
         * ladder rather than a rung of it, and smokes exactly as it did in 2.1 — no tier scales its effects
         * or the hit's odds.
         */
        public boolean untiered() {
            return strain.is(ModStrains.SCHWAG);
        }

        /** This entry stamped {@code chemotype}, keeping everything else. */
        public Entry withStamp(Chemotype chemotype) {
            return new Entry(strain, count, chemotype, potency, tier);
        }

        /**
         * This entry with its plant's buds' band offsets summing to {@code potency}; anything that is
         * not a plant's buds has none.
         */
        public Entry withPotency(int potency) {
            return strain.value().wildFlower().isPresent() ? new Entry(strain, count, stamp, potency, tier) : this;
        }

        /**
         * What this entry is. <b>A plant's buds are their strain's chemotype</b>, whatever the entry
         * says, so a Beldía spliff 2.1 rolled reads balanced like every Beldía bud; the stamp speaks for
         * the matter that has no plant of its own to ask (schwag, scorched hemp, the hash family).
         */
        public Chemotype chemotype() {
            Strain value = strain.value();
            return value.wildFlower().isPresent() ? value.chemotype() : stamp;
        }

        /**
         * The THC in this entry, in halves of a THC-dominant mid bud's, times {@link Potency#DEFAULT_BASE}:
         * its chemotype's THC times what it is worth in decarboxylated hemp. A plant's buds are worth
         * what they would bake into, the strain's base each moved by its band ({@link #potency}), never
         * less than one each; anything else the default base per item, so schwag and hash weigh what they
         * always did.
         */
        public long thcWeight() {
            return worth() * chemotype().thcHalves();
        }

        /**
         * The CBD in this entry, weighed as {@link #thcWeight} weighs its THC: the band moves the CBD
         * too, since a plant's total cannabinoids are polygenic and the chemotype only splits them.
         */
        public long cbdWeight() {
            return worth() * chemotype().cbdHalves();
        }

        /** What this entry is worth in decarboxylated hemp, the number both weights multiply. */
        private long worth() {
            Strain value = strain.value();
            return value.wildFlower().isPresent()
                    ? Math.max(count, (long) count * value.potency().base() + potency)
                    : (long) count * Potency.DEFAULT_BASE;
        }

        /**
         * This entry's THC as an exact dose, unrounded: a mid THC-dominant bud is 1.0, a high one 1.25,
         * a balanced one 0.5. {@link #thcDose} rounds it down for the level; the rest buys time.
         */
        public double thcUnits() {
            return units(thcWeight());
        }

        /** This entry's CBD as an exact dose, unrounded: a mid CBD-dominant bud is 1.0. */
        public double cbdUnits() {
            return units(cbdWeight());
        }

        /**
         * This entry's THC dose: its count weighed by its chemotype and its bands, rounded down, never
         * below one with any THC. For a THC-dominant mid bud it is the count, as before bands existed.
         */
        public int thcDose() {
            return dose(thcWeight(), Integer.MAX_VALUE);
        }
    }

    /** A THC weight ({@link Entry#thcWeight}) as a dose: rounded down, at least one with any THC, at most {@code cap}. */
    private static int dose(long weight, int cap) {
        if (weight <= 0) {
            return 0;
        }
        long dose = weight / ((long) Chemotype.FULL_WEIGHT * Potency.DEFAULT_BASE);
        return (int) Math.min(cap, Math.max(1, dose));
    }

    /** A weight as an exact dose: one is a mid THC-dominant bud's worth of THC (or a CBD bud's of CBD). */
    private static double units(long weight) {
        return weight / (double) ((long) Chemotype.FULL_WEIGHT * Potency.DEFAULT_BASE);
    }

    /** The most the exact dose may stretch a THC-labelled effect: twice the device's duration. */
    public static final double MAX_THC_STRETCH = 2.0;

    /**
     * How long a THC-labelled effect of an entry lasts: <b>the level rounds down, the exact dose buys
     * time</b>. With {@code units} the entry's exact THC dose D, its level ⌊D⌋ (at least one) and
     * {@code b} that level held to the buff cap, the device's duration stretches by
     * {@code 1 + (D - b) / 2}, at most {@link #MAX_THC_STRETCH} times. A mid bud, a mid pipe of two and
     * a mid bong of three (D = 3, b = 2, one and a half) last exactly what they always did; a high bud
     * lasts an eighth longer, a low one an eighth shorter, and a balanced bong of three, D = 1.5, a
     * quarter longer rather than a half: it has less THC in it. Vanilla's grammar, where glowstone buys
     * level and redstone buys time.
     */
    public static int thcTicks(int durationTicks, double units, int level, int buffCap) {
        int held = Math.min(level, buffCap);
        double stretch = Math.min(MAX_THC_STRETCH, 1.0 + (units - held) / 2.0);
        return (int) Math.round(durationTicks * stretch);
    }

    /** Ticks of a CBD-labelled smoke effect per whole unit of CBD smoked: one Regeneration I heal. */
    public static final int CBD_TICKS_PER_UNIT = 50;
    /** The longest a CBD-labelled smoke effect runs, whatever the device: three units' worth. */
    public static final int MAX_CBD_TICKS = 150;

    /**
     * How long a CBD-labelled smoke effect lasts, whatever the device: {@link #CBD_TICKS_PER_UNIT} per
     * unit of the exact CBD dose, at most {@link #MAX_CBD_TICKS}. Level I always. Edibles stay the
     * stronger CBD route (five to twenty seconds of Regeneration), as they last longer in life.
     */
    public static int cbdTicks(double units) {
        return (int) Math.min(MAX_CBD_TICKS, Math.round(CBD_TICKS_PER_UNIT * units));
    }

    /** A single-strain load. */
    public static SmokeContents of(Holder<Strain> strain, int count) {
        return new SmokeContents(List.of(new Entry(strain, count)));
    }

    /** A single-strain load of stamped matter (schwag, scorched hemp, hash); a plant's buds ignore the stamp. */
    public static SmokeContents of(Holder<Strain> strain, int count, Chemotype stamp) {
        return new SmokeContents(List.of(new Entry(strain, count, stamp)));
    }

    /** A single-strain load whose buds, if a plant's, have band offsets summing to {@code potency}. */
    public static SmokeContents of(Holder<Strain> strain, int count, Chemotype stamp, int potency) {
        return new SmokeContents(List.of(new Entry(strain, count, stamp).withPotency(potency)));
    }

    /** A single-strain load whose buds, if a plant's, are all of {@code band}. */
    public static SmokeContents of(Holder<Strain> strain, int count, Chemotype stamp, PotencyBand band) {
        return of(strain, count, stamp, count * band.offset());
    }

    /**
     * This load with its plant entries at {@code budTier} and its other entries at {@code resinTier},
     * each where given. A spliff or a moon rock keeps the two apart: the weakest of its buds, the weakest
     * of its hash.
     */
    public SmokeContents withTiers(Optional<BudTier> budTier, Optional<BudTier> resinTier) {
        List<Entry> tiered = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            Optional<BudTier> tier = entry.strain().value().wildFlower().isPresent() ? budTier : resinTier;
            tiered.add(tier.map(entry::withTier).orElse(entry));
        }
        return new SmokeContents(tiered);
    }

    /**
     * The tier of the primary entry ({@link #primaryStrain}), which sets the odds a hit rolls once for the
     * whole load — its cough, its Nausea and its green-out — as it sets their strain factors. Empty when the
     * load is empty or its primary entry smokes outside the tiers (schwag): the odds are then 2.1's.
     */
    public Optional<BudTier> primaryTier() {
        Entry best = primaryEntry();
        return best == null || best.untiered() ? Optional.empty() : Optional.of(best.tier());
    }

    /** This load with its plant's buds' band offsets summing to {@code potency}. */
    public SmokeContents withPotency(int potency) {
        List<Entry> banded = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            banded.add(entry.withPotency(potency));
        }
        return new SmokeContents(banded);
    }

    /**
     * A plant strain plus a pinch of hash — the two-entry load a hash spliff carries.
     *
     * <p>The plant goes first so it is unambiguously the primary entry, but nothing depends on the
     * order: {@link #primaryStrain} picks by count and a hash spliff is 2 buds against 1 hash.
     */
    public static SmokeContents of(Holder<Strain> strain, int count,
                                   Holder<Strain> additive, int additiveCount) {
        return new SmokeContents(List.of(new Entry(strain, count), new Entry(additive, additiveCount)));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Total units loaded: how much is burned. It names the item ("Bong III") and drives the smoke's
     * own effects; the THC in it is {@link #thcDose}, which is what drives the high and the green-out.
     * For a THC-dominant load the two are the same number.
     */
    public int dose() {
        int total = 0;
        for (Entry entry : entries) {
            total += entry.count();
        }
        return total;
    }

    /** The most a bowl's THC dose can be: a full green-out, and a bong of three THC-dominant buds. */
    public static final int MAX_THC_DOSE = 3;

    /**
     * The THC in the load as a dose: each entry's units weighed by its chemotype's THC (a balanced
     * bud is half a THC-dominant one) and a bud's by its potency band (a high bud of base 4 bakes into
     * 5 decarboxylated hemp, so it is 5/4 of a mid one), summed and rounded down, <b>never below one
     * when anything in it has THC</b>, and never above {@link #MAX_THC_DOSE}. So a pipe of two
     * extra-high buds is a full green-out, and buds of mixed bands come out at their average. A load with no THC is dose 0:
     * no high, no green-out, only the smoke and the cough.
     */
    public int thcDose() {
        long weight = 0;
        for (Entry entry : entries) {
            weight += entry.thcWeight();
        }
        return dose(weight, MAX_THC_DOSE);
    }

    /**
     * The dominant entry, by count. {@code null} only when empty.
     *
     * <p><b>A tie goes to the plant.</b> The dose-2 hash spliff is one bud and one pinch, so the two
     * entries are level and "whichever came first" would decide the item's name, its tint and which
     * one {@link #hashAdditive} reports — from list order alone, which an NBT edit or a re-encode
     * could flip. The plant is the identity in every load that has one, so it wins the tie
     * explicitly rather than by construction.
     */
    private Entry primaryEntry() {
        Entry best = null;
        for (Entry entry : entries) {
            if (best == null || entry.count() > best.count()
                    || (entry.count() == best.count()
                        && entry.strain().value().wildFlower().isPresent()
                        && best.strain().value().wildFlower().isEmpty())) {
                best = entry;
            }
        }
        return best;
    }

    /** The dominant strain, for naming and tinting. {@code null} only when empty. */
    public Holder<Strain> primaryStrain() {
        Entry best = primaryEntry();
        return best == null ? null : best.strain();
    }

    /**
     * The level the primary entry's effects actually come out at. <b>Not {@link #dose()}.</b>
     *
     * <p>{@code Strain.effects(count, …)} reads each entry's own count, so a spliff of 2 buds and 1
     * hashish is dose 3 but the strain lands at level <b>II</b>. The Roman numeral in the item's
     * name has always meant "the level of the strain's effects" and has to keep meaning that —
     * anything else is the name lying about what the item does. <b>For a single-entry load the two
     * numbers are identical</b>, so nothing already in a world changes.
     */
    public int primaryCount() {
        Entry best = primaryEntry();
        return best == null ? 0 : best.count();
    }

    /**
     * The hash-family entry riding along with a plant strain, if there is one.
     *
     * <p>"Hash-family" is spelled {@code wildFlower().isEmpty()} — the mod-wide predicate for "this did
     * not grow on a plant" — so charas and filtered hashish are covered without being named, and so
     * is anything hash-shaped a datapack adds. Empty for a plain single-strain load, and empty for a
     * genuine multi-plant blend, which is a different case that keeps the "Mixed" name.
     */
    public Optional<Holder<Strain>> hashAdditive() {
        if (entries.size() != 2) {
            return Optional.empty();
        }
        Entry primary = primaryEntry();
        // Hoisted: this does not vary with the entry being examined, and reading it inside the loop
        // suggests it does. A load whose primary is itself strainless (two hash entries, which only
        // an edited stack produces) has no plant to name, so it stays a plain "Mixed" blend.
        if (primary.strain().value().wildFlower().isEmpty()) {
            return Optional.empty();
        }
        for (Entry entry : entries) {
            if (entry != primary && entry.strain().value().wildFlower().isEmpty()) {
                return Optional.of(entry.strain());
            }
        }
        return Optional.empty();
    }

    /** True once more than one strain is loaded — naming and tinting both branch on this. */
    public boolean isBlend() {
        return entries.size() > 1;
    }

    /**
     * Average of the loaded strains' colours, weighted by bud count — the same approach
     * {@code PotionContents#getColor} takes across a potion's effects. Falls back to a
     * neutral tint when empty so a colour provider never has to null-check.
     */
    public int color() {
        if (entries.isEmpty()) {
            return 0xFFFFFF;
        }
        int r = 0, g = 0, b = 0, total = 0;
        for (Entry entry : entries) {
            int color = entry.strain().value().color();
            int weight = Math.max(1, entry.count());
            r += ((color >> 16) & 0xFF) * weight;
            g += ((color >> 8) & 0xFF) * weight;
            b += (color & 0xFF) * weight;
            total += weight;
        }
        return ((r / total) << 16) | ((g / total) << 8) | (b / total);
    }

    /**
     * The display name of what is loaded — a strain name, or "Mixed" for a genuine blend.
     *
     * <p><b>A strain-plus-hash load names the strain.</b> The plant is the identity and the hash is a
     * modifier, which belongs in the tooltip rather than in the name — the same line vanilla draws
     * between "Potion of Strength" (the payload is what the item <em>is</em>) and a shulker box
     * listing its contents (the payload is what is <em>in</em> it). A packed spliff is a container.
     *
     * <p>{@code hempdustry.strain.blend} stays reserved for a load with more than one <em>plant</em>
     * strain in it, which is the case where there is no primary strain to name it after.
     */
    public Component loadName() {
        if (isBlend() && hashAdditive().isEmpty()) {
            return Component.translatable("hempdustry.strain.blend");
        }
        Holder<Strain> strain = primaryStrain();
        return strain == null ? Component.empty() : Component.translatable(strain.value().translationKey());
    }

    /**
     * The item name for a loaded device or spliff — "Purple Kush Bong III".
     *
     * <p>Built potion-style from a per-device format key plus the load's own name, so a new strain
     * costs <b>one</b> lang key across all eight locales instead of one per device. The format string
     * being per-locale is what lets French say "Bang de %s". The level suffix reuses vanilla's
     * {@code enchantment.level.N} keys and is omitted at dose 1, exactly as vanilla writes "Potion of
     * Strength" and "Potion of Strength II".
     */
    public static Component packedName(String formatKey, SmokeContents contents) {
        MutableComponent name = Component.translatable(formatKey, contents.loadName());
        // The primary entry's count, not the total: see primaryCount(). Identical for every
        // single-entry load, so no name already in a world changes.
        int level = contents.primaryCount();
        if (level > 1) {
            name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
        }
        return name;
    }

    /**
     * Every status effect one hit of this load applies on the hit ({@code onExhale} false) or with
     * the exhale ({@code onExhale} true), before the config's {@code EffectPolicy}. An effect with a
     * {@code chance} below one is rolled on {@code random}, once per call.
     *
     * <p><b>THC-labelled effects: the level rounds down, the exact dose buys time</b>
     * ({@link #thcTicks}). A bong of three mid buds still lasts 75 s instead of 50, and a band moves it:
     * a high bud lasts longer than a mid one, a balanced bong less long than a THC one.
     *
     * <p><b>Strain- and smoke-labelled effects: a bud past the buff cap buys time, not level</b>: each
     * one over {@link EffectPolicy#maxBuffLevel} adds half the device's duration, counted in buds and
     * never in THC, so a band never lengthens Beldía's Invisibility. Counted per entry, so a
     * two-and-one mix, each strain at its own count, has nothing past the cap.
     *
     * <p><b>CBD-labelled effects</b> run at level I for {@link #cbdTicks} of the entry's CBD, whatever
     * the device and the tier.
     *
     * <p><b>Each entry's tier scales the device's duration for that entry alone</b>
     * ({@link BudTier#durationFactor}): Mids three quarters, Dank as 2.1, Top Shelf a quarter more, Epic
     * half again, applied before the rules above, so the band's stretch and its cap work on the tiered
     * duration.
     *
     * <p><b>A bud loaded with a concentrate goes one level past the cap</b> (see {@link #buffBonus}):
     * its scaling buffs rise by one and may reach {@code maxBuffLevel + 1}. Every other entry, the
     * concentrate's own strain-agnostic effects included, is held to the plain cap here, so the
     * raised cap {@code Smoking} hands the policy lifts only the bud.
     */
    public List<MobEffectInstance> effects(int durationTicks, boolean onExhale, RandomGenerator random) {
        int load = buffBonus();
        List<MobEffectInstance> out = new ArrayList<>();
        for (Entry entry : entries) {
            int bonus = entry.strain().value().wildFlower().isPresent() ? load : 0;
            int cap = EffectPolicy.maxBuffLevel(bonus);
            int past = Math.max(0, entry.count() - cap);
            // The entry's tier scales the device's duration for its own effects; times the band's stretch,
            // an extra-high Epic bud lasts three times the device. A CBD effect keeps its own clock.
            int lasting = entry.untiered() ? durationTicks : entry.tier().scaleDuration(durationTicks);
            Strain.Burn burn = new Strain.Burn(entry.count(), entry.thcDose(), entry.chemotype(),
                    thcTicks(lasting, entry.thcUnits(), entry.thcDose(), cap),
                    lasting + lasting * past / 2,
                    cbdTicks(entry.cbdUnits()));
            for (MobEffectInstance effect : entry.strain().value().effects(burn, onExhale, bonus, random)) {
                out.add(new MobEffectInstance(effect.getEffect(), effect.getDuration(),
                        EffectPolicy.amplifier(effect.getEffect(), effect.getAmplifier(), bonus)));
            }
        }
        return out;
    }

    /**
     * 2.0's question: everything one hit of this load applies, the hit's effects and the exhale's
     * together, and every effect with a chance taken as landing (a chance of zero still never does).
     *
     * @deprecated kept so an addon built against 2.0 still links; use
     *             {@link #effects(int, boolean, RandomGenerator)}
     */
    @Deprecated
    public List<MobEffectInstance> effects(int durationTicks) {
        // A generator whose every draw is zero: nextFloat() is 0.0, under any chance above zero.
        RandomGenerator lands = () -> 0L;
        List<MobEffectInstance> out = effects(durationTicks, false, lands);
        out.addAll(effects(durationTicks, true, lands));
        return out;
    }

    /**
     * One when a bud is loaded with a concentrate, else none: the only way past the buff cap.
     *
     * <p>A moon rock is described in the trade as flower for character, concentrate for potency,
     * and that is the rule: the concentrate lifts the plant's own effects rather than adding a
     * bigger version of its own. A concentrate is a strain with no flower and a buff of its own,
     * which is every hash and rosin and not scorched hemp. <b>Once</b>: two concentrates do not make
     * a fourth level, because this is a flag on the load and not a count.
     */
    public int buffBonus() {
        boolean bud = false;
        boolean concentrate = false;
        for (Entry entry : entries) {
            Strain strain = entry.strain().value();
            if (strain.wildFlower().isPresent()) {
                bud = true;
            } else if (strain.smokeEffects().stream().anyMatch(effect ->
                    effect.effect().value().getCategory() == MobEffectCategory.BENEFICIAL)) {
                concentrate = true;
            }
        }
        return bud && concentrate ? 1 : 0;
    }
}
