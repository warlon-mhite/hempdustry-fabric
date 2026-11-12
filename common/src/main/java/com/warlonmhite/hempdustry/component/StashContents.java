package com.warlonmhite.hempdustry.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.block.custom.stash.StashPieces;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.util.ModTags;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * What a Stash Jar holds ({@code hempdustry:stash_contents}): an ordered list of entries, the last one
 * put in first, as a vanilla bundle keeps its stacks.
 *
 * <h2>The jar blends</h2>
 *
 * Every bud of one strain is one {@link Entry}, whatever tier and potency band it came in: the entry keeps
 * how many buds there are and the exact sums of their tiers (Mids 0 to Epic 3) and bands (Low −1 to
 * Extra-High +2), and lays them out as evenly as whole steps allow ({@link Entry#parts}). So nothing is
 * made or lost (the steps out add up to the steps in, and the oven bakes the same decarboxylated hemp from
 * them), nothing comes out past the best or the worst that went in, the order things went in does not
 * matter, and taking buds out and putting them back changes nothing. Strains never blend with each other
 * and a chemotype never blends either: schwag is an entry per chemotype. A stack carrying anything else
 * (a name from an anvil, another mod's data) is kept exactly as it is, an entry of its own.
 *
 * <p>Not vanilla's {@code bundle_contents}, because a bundle is measured in weight and holds one stack's
 * worth at most. A jar is measured in items, and how many is the jar's, not the component's: the same
 * list fits a Stash Jar, a Gilded one or a Diamond one, so an upgrade only changes the item around it.
 *
 * <p>The selected entry is the one the inventory's scroll menu points at. Like the bundle's it is never
 * saved and plays no part in equality, so selecting does not make two jars different.
 */
public final class StashContents {
    public static final StashContents EMPTY = new StashContents(List.of());
    public static final Codec<StashContents> CODEC = Entry.CODEC.listOf()
            .xmap(StashContents::new, StashContents::entries);
    public static final StreamCodec<RegistryFriendlyByteBuf, StashContents> STREAM_CODEC = Entry.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(StashContents::new, StashContents::entries);

    /** No entry selected. */
    public static final int NONE = -1;

    private final List<Entry> entries;
    private final int selected;
    private final int count;
    // The pieces' colours, worked out once per value: the item's tints ask every frame.
    private volatile @Nullable Pieces pieces;

    private StashContents(List<Entry> entries) {
        this(entries, NONE);
    }

    private StashContents(List<Entry> entries, int selected) {
        this.entries = List.copyOf(entries);
        this.selected = selected;
        int total = 0;
        for (Entry entry : this.entries) {
            total += entry.count();
        }
        this.count = total;
    }

    /** Whether {@code stack} may go into a jar at all: the tag, and nothing else. */
    public static boolean storable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.Items.STASH_JAR_STORABLE);
    }

    /** The entries, the last one put in first. */
    public List<Entry> entries() {
        return entries;
    }

    /** Every stack that would come out, entry by entry (newest first), each entry's best first; stacks may pass their item's stack size. */
    public List<ItemStack> parts() {
        List<ItemStack> parts = new ArrayList<>();
        for (Entry entry : entries) {
            parts.addAll(entry.parts());
        }
        return parts;
    }

    /** Every stack that would come out, cut to stack size: what a jar destroyed as an item lets out. */
    public List<ItemStack> spill() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack part : parts()) {
            int max = part.getMaxStackSize();
            for (int left = part.getCount(); left > 0; left -= max) {
                out.add(part.copyWithCount(Math.min(left, max)));
            }
        }
        return out;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Items inside, every entry counted by its items, whatever their stack size. */
    public int count() {
        return count;
    }

    public int size() {
        return entries.size();
    }

    public int selected() {
        return selected;
    }

    public @Nullable Entry selectedEntry() {
        return selected < 0 || selected >= entries.size() ? null : entries.get(selected);
    }

    /** How many entries the inventory's grid shows, the bundle's rule (twelve, or eleven and a count). */
    public int shown() {
        int stacks = entries.size();
        int available = stacks > 12 ? 11 : 12;
        int onLastRow = stacks % 4;
        int gaps = onLastRow == 0 ? 0 : 4 - onLastRow;
        return Math.min(stacks, available - gaps);
    }

    /** How full a jar of {@code capacity} items is, 0 to 1. */
    public float fullness(int capacity) {
        return capacity <= 0 ? 1.0F : Mth.clamp((float) count / capacity, 0.0F, 1.0F);
    }

    /** These contents with entry {@code index} selected, or the selection cleared if it already was or is out of range. */
    public StashContents toggleSelected(int index) {
        int next = index != selected && index >= 0 && index < entries.size() ? index : NONE;
        return next == selected ? this : new StashContents(entries, next);
    }

    /** These contents with no entry selected: what a jar set down or moved keeps, so its next stack out is the newest. */
    public StashContents withoutSelection() {
        return selected == NONE ? this : new StashContents(entries, NONE);
    }

    /**
     * The body and pistil colours of each stash piece a jar of {@code capacity} holding these shows,
     * largest share first ({@link StashPieces}). Kept on the value, which never changes, for the capacity
     * and registries last asked.
     */
    public StashPieces.Colours pieceColours(int capacity, @Nullable HolderLookup.Provider registries) {
        Pieces memo = pieces;
        if (memo == null || memo.capacity != capacity || memo.registries != registries) {
            memo = new Pieces(capacity, registries, StashPieces.colours(this, capacity, registries));
            pieces = memo;
        }
        return memo.colours;
    }

    private record Pieces(int capacity, @Nullable HolderLookup.Provider registries, StashPieces.Colours colours) {
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof StashContents contents && entries.equals(contents.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return "StashContents" + entries;
    }

    /**
     * One kind in a jar: {@code item} (count one) with every component that does not blend, and how many,
     * with the exact sums of their tier ranks and band offsets.
     *
     * <p>An entry <em>blends</em> when its item carries nothing but a chemotype: every bud and schwag a
     * plant gave. Its buds come out spread as evenly as whole steps allow: with {@code q = ⌊T / n⌋} and
     * {@code r = T − q·n}, {@code r} buds at tier {@code q + 1} and the rest at {@code q}, the same for
     * bands, the best tier paired with the best band, so an entry holds at most two neighbouring tiers and
     * two neighbouring bands, three different stacks at most. Taking any of those stacks out leaves the
     * rest laid out the same way, with nothing to work out again. Any other entry is an exact stack: its
     * item keeps its tier, band and everything else, and its sums only follow from them.
     */
    public record Entry(ItemStackTemplate item, int count, int tierSum, int bandSum) {
        /** An absent sum reads Dank and Mid; an exact entry's sums are its item's own; a sum past the four tiers or bands is held to them. */
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStackTemplate.CODEC.fieldOf("item").forGetter(Entry::item),
                ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(Entry::count),
                Codec.INT.optionalFieldOf("tier_sum").forGetter(entry -> entry.blends() ? Optional.of(entry.tierSum) : Optional.empty()),
                Codec.INT.optionalFieldOf("band_sum").forGetter(entry -> entry.blends() ? Optional.of(entry.bandSum) : Optional.empty())
        ).apply(instance, (item, count, tier, band) -> of(item.withCount(1), count,
                tier.orElse(count * BudTier.DANK.rank()), band.orElse(PotencyBand.MID.offset()))));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, Entry::item,
                ByteBufCodecs.VAR_INT, Entry::count,
                ByteBufCodecs.VAR_INT, Entry::tierSum,
                ByteBufCodecs.VAR_INT, Entry::bandSum,
                Entry::fromWire);

        /**
         * An entry as a packet carries it, refused below one item as {@link #CODEC} refuses it in a save:
         * a creative client chooses the components of the stacks it sends, and an entry of no buds lays
         * out nothing to divide (a pipe reading its parts would stop the server).
         */
        private static Entry fromWire(ItemStackTemplate item, int count, int tierSum, int bandSum) {
            if (count < 1) {
                throw new DecoderException("A Stash Jar entry holds at least one item, not " + count);
            }
            return of(item, count, tierSum, bandSum);
        }

        /**
         * An entry with these values; an exact one takes its sums from its item, whatever was passed, and a
         * blending one's sums are held between all Mids, Low and all Epic, Extra-High.
         */
        public static Entry of(ItemStackTemplate item, int count, int tierSum, int bandSum) {
            ItemStackTemplate one = item.withCount(1);
            if (!blends(one)) {
                ItemStack stack = one.create();
                return new Entry(one, count, count * BudTier.of(stack).rank(), count * PotencyBand.of(stack).offset());
            }
            return new Entry(one, count,
                    Mth.clamp(tierSum, count * BudTier.MIDS.rank(), count * BudTier.EPIC.rank()),
                    Mth.clamp(bandSum, count * PotencyBand.LOW.offset(), count * PotencyBand.EXTRA_HIGH.offset()));
        }

        /** Whether an entry of {@code item} blends: it carries no component but a chemotype. */
        private static boolean blends(ItemStackTemplate item) {
            return item.components().forget(type -> type == ModComponents.CHEMOTYPE).isEmpty();
        }

        /** Whether this entry's buds blend, or it is one exact stack. */
        public boolean blends() {
            return blends(item);
        }

        /**
         * The stacks this entry gives back, best first: the top tier with the top band. At most three for
         * a blending entry, one for an exact one; each stack may pass its item's stack size.
         */
        public List<ItemStack> parts() {
            if (!blends()) {
                return List.of(item.create().copyWithCount(count));
            }
            int tier = Math.floorDiv(tierSum, count);
            int tierUp = tierSum - tier * count;
            int band = Math.floorDiv(bandSum, count);
            int bandUp = bandSum - band * count;
            int first = Math.min(tierUp, bandUp);
            int second = Math.max(tierUp, bandUp);
            List<ItemStack> parts = new ArrayList<>(3);
            addPart(parts, first, tier + 1, band + 1);
            addPart(parts, second - first, tierUp > bandUp ? tier + 1 : tier, bandUp > tierUp ? band + 1 : band);
            addPart(parts, count - second, tier, band);
            return parts;
        }

        private void addPart(List<ItemStack> parts, int n, int tierRank, int bandOffset) {
            if (n > 0) {
                parts.add(stamped(item, n, BudTier.ofRank(tierRank), bandOf(bandOffset)));
            }
        }

        /** The first stack out, as a grid shows it: the best buds, counted as the whole entry. */
        public ItemStack icon() {
            return parts().getFirst().copyWithCount(count);
        }

        /** This entry with {@code stack} added; {@code stack} must be of its kind ({@link #keyOf}). */
        private Entry plus(ItemStack stack, int n) {
            return new Entry(item, count + n, tierSum + n * BudTier.of(stack).rank(), bandSum + n * PotencyBand.of(stack).offset());
        }

        /** This entry without {@code n} of {@code part}, one of its own parts; null once nothing is left. */
        private @Nullable Entry minus(ItemStack part, int n) {
            if (n >= count) {
                return null;
            }
            return new Entry(item, count - n, tierSum - n * BudTier.of(part).rank(), bandSum - n * PotencyBand.of(part).offset());
        }
    }

    /**
     * The entry key {@code stack} files under: its item with its blending components set aside, when the
     * stack is exactly what that key would give back at its tier and band; otherwise the stack itself,
     * kept exact.
     */
    static ItemStackTemplate keyOf(ItemStack stack) {
        DataComponentPatch rest = stack.getComponentsPatch().forget(type -> type == ModComponents.BUD_TIER
                || type == ModComponents.POTENCY_BAND || type == DataComponents.RARITY);
        ItemStackTemplate key = new ItemStackTemplate(stack.typeHolder(), 1, rest);
        if (Entry.blends(key)
                && ItemStack.isSameItemSameComponents(stamped(key, 1, BudTier.of(stack), PotencyBand.of(stack)), stack)) {
            return key;
        }
        return ItemStackTemplate.fromNonEmptyStack(stack, 1);
    }

    private static ItemStack stamped(ItemStackTemplate key, int n, BudTier tier, PotencyBand band) {
        ItemStack stack = key.create().copyWithCount(n);
        BudTier.stamp(stack, tier);
        PotencyBand.stamp(stack, band);
        return stack;
    }

    private static PotencyBand bandOf(int offset) {
        for (PotencyBand band : PotencyBand.values()) {
            if (band.offset() == offset) {
                return band;
            }
        }
        return offset < PotencyBand.LOW.offset() ? PotencyBand.LOW : PotencyBand.EXTRA_HIGH;
    }

    /** A working copy: what the jar's every way in and out edits, then freezes back. */
    public static final class Mutable {
        private final List<Entry> entries;
        private int selected;
        private int count;

        private Mutable(StashContents from) {
            this.entries = new ArrayList<>(from.entries);
            this.selected = from.selected;
            this.count = from.count;
        }

        public int count() {
            return count;
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        /**
         * Puts as much of {@code stack} in as {@code capacity} leaves room for, shrinking it by what
         * went in, and returns that many. Refuses anything {@link #storable} refuses. The items blend into
         * their strain's entry, which then moves to the front as a bundle's stack does, or start one there.
         */
        public int insert(ItemStack stack, int capacity) {
            if (!storable(stack)) {
                return 0;
            }
            return add(stack, Math.min(stack.getCount(), capacity - count));
        }

        /**
         * Puts {@code stack}, just taken out, back in whole, whatever the tag or the room say: a stack
         * that came out of the jar and found no place to go must not be lost because a datapack changed
         * the tag in between. Its sums go back exactly, so the entry is what it was.
         */
        public void putBack(ItemStack stack) {
            add(stack, stack.getCount());
        }

        private int add(ItemStack stack, int moving) {
            if (moving <= 0 || stack.isEmpty()) {
                return 0;
            }
            ItemStackTemplate key = keyOf(stack);
            Entry entry = null;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).item().equals(key)) {
                    entry = entries.remove(i).plus(stack, moving);
                    break;
                }
            }
            if (entry == null) {
                entry = Entry.of(key, 0, 0, 0).plus(stack, moving);
            }
            entries.addFirst(entry);
            stack.shrink(moving);
            count += moving;
            selected = NONE;
            return moving;
        }

        /**
         * Takes out the selected entry's best stack, else the last entry's, up to a stack of its item;
         * empty when there is none. Clears the selection.
         */
        public ItemStack removeOne() {
            if (entries.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int index = selected >= 0 && selected < entries.size() ? selected : 0;
            selected = NONE;
            ItemStack best = entries.get(index).parts().getFirst();
            return take(index, best, Math.min(best.getCount(), best.getMaxStackSize()));
        }

        /**
         * Takes up to {@code max} items that {@code matches} accepts, newest entries first and each
         * entry's best first, and returns them, one stack per kind taken. A pipe's way out: it names what
         * it wants and how much.
         */
        public List<ItemStack> extract(Predicate<ItemStack> matches, int max) {
            List<ItemStack> out = new ArrayList<>();
            int taken = 0;
            for (int i = 0; i < entries.size() && taken < max; ) {
                ItemStack part = null;
                for (ItemStack candidate : entries.get(i).parts()) {
                    if (matches.test(candidate)) {
                        part = candidate;
                        break;
                    }
                }
                if (part == null) {
                    i++;
                    continue;
                }
                int n = Math.min(part.getCount(), max - taken);
                boolean emptied = n >= entries.get(i).count();
                out.add(take(i, part, n));
                taken += n;
                if (!emptied && n == part.getCount()) {
                    continue; // the next part of the same entry may match too
                }
                if (!emptied) {
                    i++;
                }
            }
            if (taken > 0) {
                selected = NONE;
            }
            return out;
        }

        private ItemStack take(int index, ItemStack part, int n) {
            Entry left = entries.get(index).minus(part, n);
            if (left == null) {
                entries.remove(index);
            } else {
                entries.set(index, left);
            }
            count -= n;
            return part.copyWithCount(n);
        }

        public StashContents toImmutable() {
            return new StashContents(ImmutableList.copyOf(entries), selected);
        }
    }
}
