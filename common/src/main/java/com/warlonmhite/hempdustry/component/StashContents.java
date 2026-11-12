package com.warlonmhite.hempdustry.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.block.custom.stash.StashPieces;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * What a Stash Jar holds ({@code hempdustry:stash_contents}): an ordered list of stacks, the last one
 * put in first, as a vanilla bundle keeps its own.
 *
 * <p>Not vanilla's {@code bundle_contents}, because a bundle is measured in weight and holds one
 * stack's worth at most. A jar is measured in items, and how many is the jar's, not the
 * component's: the same list fits a Stash Jar, a Gilded one or a Diamond one, so an upgrade only
 * changes the item around it. Each entry is at most one stack of its item, since a stack template
 * cannot count past 99: 256 identical buds are four entries.
 *
 * <p>The selected entry is the one the inventory's scroll menu points at. Like the bundle's it is
 * never saved and plays no part in equality, so selecting does not make two jars different.
 */
public final class StashContents {
    public static final StashContents EMPTY = new StashContents(List.of());
    public static final Codec<StashContents> CODEC = ItemStackTemplate.CODEC.listOf()
            .xmap(StashContents::new, StashContents::items);
    public static final StreamCodec<RegistryFriendlyByteBuf, StashContents> STREAM_CODEC = ItemStackTemplate.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(StashContents::new, StashContents::items);

    /** No entry selected. */
    public static final int NONE = -1;

    private final List<ItemStackTemplate> items;
    private final int selected;
    private final int count;
    // The pieces' colours, worked out once per value: the item's tints ask every frame.
    private volatile @Nullable Pieces pieces;

    public StashContents(List<ItemStackTemplate> items) {
        this(items, NONE);
    }

    private StashContents(List<ItemStackTemplate> items, int selected) {
        this.items = List.copyOf(items);
        this.selected = selected;
        int total = 0;
        for (ItemStackTemplate item : this.items) {
            total += item.count();
        }
        this.count = total;
    }

    /** Whether {@code stack} may go into a jar at all: the tag, and nothing else. */
    public static boolean storable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.Items.STASH_JAR_STORABLE);
    }

    /** The entries, the last one put in first. */
    public List<ItemStackTemplate> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Items inside, every entry counted by its items, whatever their stack size. */
    public int count() {
        return count;
    }

    public int size() {
        return items.size();
    }

    public int selected() {
        return selected;
    }

    public @Nullable ItemStackTemplate selectedItem() {
        return selected < 0 || selected >= items.size() ? null : items.get(selected);
    }

    /** How many entries the inventory's grid shows, the bundle's rule (twelve, or eleven and a count). */
    public int shown() {
        int stacks = items.size();
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
        int next = index != selected && index >= 0 && index < items.size() ? index : NONE;
        return next == selected ? this : new StashContents(items, next);
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

    /** These contents with no entry selected: what a jar set down or moved keeps, so its next stack out is the newest. */
    public StashContents withoutSelection() {
        return selected == NONE ? this : new StashContents(items, NONE);
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof StashContents contents && items.equals(contents.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        return "StashContents" + items;
    }

    /** A working copy: what the jar's every way in and out edits, then freezes back. */
    public static final class Mutable {
        private final List<ItemStack> stacks = new ArrayList<>();
        private int selected;
        private int count;

        private Mutable(StashContents from) {
            for (ItemStackTemplate item : from.items) {
                stacks.add(item.create());
            }
            this.selected = from.selected;
            this.count = from.count;
        }

        public int count() {
            return count;
        }

        public boolean isEmpty() {
            return stacks.isEmpty();
        }

        /**
         * Puts as much of {@code stack} in as {@code capacity} leaves room for, shrinking it by what
         * went in, and returns that many. Refuses anything {@link #storable} refuses. The items join
         * the newest entry of their kind that has room, which then moves to the front as a bundle's
         * does; whatever is left starts new entries in front of it.
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
         * the tag in between.
         */
        public void putBack(ItemStack stack) {
            add(stack, stack.getCount());
        }

        private int add(ItemStack stack, int moving) {
            if (moving <= 0 || stack.isEmpty()) {
                return 0;
            }
            int max = stack.getMaxStackSize();
            int left = moving;
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack entry = stacks.get(i);
                if (entry.getCount() < max && ItemStack.isSameItemSameComponents(entry, stack)) {
                    stacks.remove(i);
                    int added = Math.min(left, max - entry.getCount());
                    entry.grow(added);
                    left -= added;
                    stacks.addFirst(entry);
                    break;
                }
            }
            while (left > 0) {
                int added = Math.min(left, max);
                stacks.addFirst(stack.copyWithCount(added));
                left -= added;
            }
            stack.shrink(moving);
            count += moving;
            selected = NONE;
            return moving;
        }

        /** Takes out the selected entry, else the last one put in; empty when there is none. Clears the selection. */
        public ItemStack removeOne() {
            if (stacks.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int index = selected >= 0 && selected < stacks.size() ? selected : 0;
            ItemStack out = stacks.remove(index);
            count -= out.getCount();
            selected = NONE;
            return out;
        }

        /**
         * Takes up to {@code max} items that {@code matches} accepts, newest entries first, and returns
         * how many came out. A pipe's way out: it names what it wants and how much.
         */
        public int extract(Predicate<ItemStack> matches, int max) {
            int taken = 0;
            for (int i = 0; i < stacks.size() && taken < max; ) {
                ItemStack entry = stacks.get(i);
                if (!matches.test(entry)) {
                    i++;
                    continue;
                }
                int out = Math.min(entry.getCount(), max - taken);
                entry.shrink(out);
                taken += out;
                if (entry.isEmpty()) {
                    stacks.remove(i);
                } else {
                    i++;
                }
            }
            count -= taken;
            if (taken > 0) {
                selected = NONE;
            }
            return taken;
        }

        public StashContents toImmutable() {
            ImmutableList.Builder<ItemStackTemplate> items = ImmutableList.builder();
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty()) {
                    items.add(ItemStackTemplate.fromNonEmptyStack(stack));
                }
            }
            return new StashContents(items.build(), selected);
        }
    }
}
