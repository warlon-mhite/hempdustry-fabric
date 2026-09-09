package com.warlonmhite.hempdustry.item.custom;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What is loaded in a spliff, pipe or bong — the payload of the {@code hempdustry:smoke_contents}
 * data component.
 *
 * <p>This is the mod's answer to {@code minecraft:potion_contents}: <b>one item per device</b> with
 * the strain carried as data, rather than a separate registered item per device × strain. See
 * CLAUDE.md §5b D10. Empty contents means an unpacked device.
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
 * and another at level I. That falls out as a broader-but-weaker trade against a single strain at
 * level III, which is roughly the fixed-budget shape mixing wants anyway — but it has not been
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
     * {@code ComponentChanges.CODEC} is a {@code dispatchedMap}, so one bad value errors the map,
     * {@code ItemStack}'s codec errors in turn, and {@code Inventories.readNbt} quietly drops the
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

    public static final PacketCodec<RegistryByteBuf, SmokeContents> PACKET_CODEC =
            Entry.PACKET_CODEC.collect(PacketCodecs.toList())
                    .xmap(SmokeContents::new, SmokeContents::entries);

    /**
     * A single strain's share of the load.
     *
     * <p>The strain is held as a {@link RegistryEntry} rather than by value: a stack stores the id
     * and resolves it against the world's strain registry, so a datapack edit reaches every spliff
     * already in a chest. {@link Strain#ENTRY_CODEC} refuses inline entries, which is what stops a
     * stack from carrying a stale copy of a definition.
     */
    public record Entry(RegistryEntry<Strain> strain, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Strain.ENTRY_CODEC.fieldOf("strain").forGetter(Entry::strain),
                Codec.INT.fieldOf("count").forGetter(Entry::count)
        ).apply(instance, Entry::new));

        public static final PacketCodec<RegistryByteBuf, Entry> PACKET_CODEC = PacketCodec.tuple(
                Strain.ENTRY_PACKET_CODEC, Entry::strain,
                PacketCodecs.VAR_INT, Entry::count,
                Entry::new);
    }

    /** A single-strain load. */
    public static SmokeContents of(RegistryEntry<Strain> strain, int count) {
        return new SmokeContents(List.of(new Entry(strain, count)));
    }

    /**
     * A plant strain plus a pinch of hash — the two-entry load a hash spliff carries.
     *
     * <p>The plant goes first so it is unambiguously the primary entry, but nothing depends on the
     * order: {@link #primaryStrain} picks by count and a hash spliff is 2 buds against 1 hash.
     */
    public static SmokeContents of(RegistryEntry<Strain> strain, int count,
                                   RegistryEntry<Strain> additive, int additiveCount) {
        return new SmokeContents(List.of(new Entry(strain, count), new Entry(additive, additiveCount)));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Total buds loaded. This is the <em>dose</em>, and it drives the amplifier and green-out odds. */
    public int dose() {
        int total = 0;
        for (Entry entry : entries) {
            total += entry.count();
        }
        return total;
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
                        && entry.strain().value().flower().isPresent()
                        && best.strain().value().flower().isEmpty())) {
                best = entry;
            }
        }
        return best;
    }

    /** The dominant strain, for naming and tinting. {@code null} only when empty. */
    public RegistryEntry<Strain> primaryStrain() {
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
     * <p>"Hash-family" is spelled {@code flower().isEmpty()} — the mod-wide predicate for "this did
     * not grow on a plant" — so charas and filtered hashish are covered without being named, and so
     * is anything hash-shaped a datapack adds. Empty for a plain single-strain load, and empty for a
     * genuine multi-plant blend, which is a different case that keeps the "Mixed" name.
     */
    public Optional<RegistryEntry<Strain>> hashAdditive() {
        if (entries.size() != 2) {
            return Optional.empty();
        }
        Entry primary = primaryEntry();
        // Hoisted: this does not vary with the entry being examined, and reading it inside the loop
        // suggests it does. A load whose primary is itself strainless (two hash entries, which only
        // an edited stack produces) has no plant to name, so it stays a plain "Mixed" blend.
        if (primary.strain().value().flower().isEmpty()) {
            return Optional.empty();
        }
        for (Entry entry : entries) {
            if (entry != primary && entry.strain().value().flower().isEmpty()) {
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
     * {@code PotionContentsComponent#getColor} takes across a potion's effects. Falls back to a
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
    public Text loadName() {
        if (isBlend() && hashAdditive().isEmpty()) {
            return Text.translatable("hempdustry.strain.blend");
        }
        RegistryEntry<Strain> strain = primaryStrain();
        return strain == null ? Text.empty() : Text.translatable(strain.value().translationKey());
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
    public static Text packedName(String formatKey, SmokeContents contents) {
        MutableText name = Text.translatable(formatKey, contents.loadName());
        // The primary entry's count, not the total: see primaryCount(). Identical for every
        // single-entry load, so no name already in a world changes.
        int level = contents.primaryCount();
        if (level > 1) {
            name.append(ScreenTexts.SPACE).append(Text.translatable("enchantment.level." + level));
        }
        return name;
    }

    /** Every status effect one hit of this load applies, lasting {@code durationTicks}. */
    public List<StatusEffectInstance> effects(int durationTicks) {
        List<StatusEffectInstance> out = new ArrayList<>();
        for (Entry entry : entries) {
            out.addAll(entry.strain().value().effects(entry.count(), durationTicks));
        }
        return out;
    }
}
