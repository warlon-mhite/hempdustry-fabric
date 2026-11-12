package com.warlonmhite.hempdustry.block.custom.stash;

import com.warlonmhite.hempdustry.component.StashContents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The colours of the stash pieces a jar shows: one piece at low, three at mid, five when full, shared
 * out by each strain's part of what is inside.
 *
 * <p>A piece is a bud: a green body with a few pistils in its strain's colour ({@link Strain#jarColourOrDefault},
 * the strain's own {@code color} unless its file names a {@code jar_colour}). The body green is one for every
 * strain, so a jar reads as a jar of weed and the strains tell themselves apart by their pistils. Schwag is
 * brown through and through, pistils and all; an item a pack put in the tag that grew on no strain is a
 * bud with the plain orange pistils most bud art has.
 *
 * <p>Every bud of one strain counts together, whatever its band, tier or resin, since those are what the
 * jar exists to put back in one slot. The pieces go by the largest remainder: each strain first gets the
 * whole pieces its share buys, and the pieces left over go to the largest fractions, so 60 % and 40 % of
 * five pieces are three and two. Largest share first, so the same mix always lays out the same way.
 */
public final class StashPieces {
    /** Every bud piece's body: the light green of the mod's bud art, shaded by the grey texture it tints. */
    public static final int BUD_GREEN = 0x93AD4C;
    /** Schwag's pieces, body and pistils: the brown of stale hay, darker than its smoke. */
    public static final int SCHWAG_COLOUR = 0x715933;
    /** The pistils of an item that grew on no strain: the orange most of the bud textures draw. */
    public static final int PLAIN_PISTILS = 0xA46D41;

    private StashPieces() {
    }

    /** Each piece's body and pistil colour, RGB, the same index in both, largest share first. */
    public record Colours(int[] bodies, int[] pistils) {
        public static final Colours NONE = new Colours(new int[0], new int[0]);

        public int count() {
            return bodies.length;
        }

        /** By value: the server sends the client new colours only when they differ. */
        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof Colours colours
                    && Arrays.equals(bodies, colours.bodies) && Arrays.equals(pistils, colours.pistils);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(bodies) + Arrays.hashCode(pistils);
        }

        @Override
        public String toString() {
            return "Colours[bodies=" + Arrays.toString(bodies) + ", pistils=" + Arrays.toString(pistils) + "]";
        }
    }

    /** The pieces a jar of {@code capacity} holding {@code contents} shows. */
    public static Colours colours(StashContents contents, int capacity, @Nullable HolderLookup.Provider registries) {
        int pieces = StashFill.of(contents.count(), capacity).pieces();
        if (pieces == 0) {
            return Colours.NONE;
        }
        Map<String, Group> groups = new LinkedHashMap<>();
        for (ItemStackTemplate item : contents.items()) {
            Group group = groupOf(item, registries);
            groups.merge(group.key, group.withCount(item.count()), Group::plus);
        }
        List<Group> sorted = new ArrayList<>(groups.values());
        sorted.sort(Comparator.comparingInt(Group::count).reversed().thenComparing(Group::key));
        int[] counts = sorted.stream().mapToInt(Group::count).toArray();
        int[] shares = share(counts, pieces);
        int[] bodies = new int[pieces];
        int[] pistils = new int[pieces];
        int at = 0;
        for (int i = 0; i < sorted.size(); i++) {
            for (int n = 0; n < shares[i]; n++) {
                bodies[at] = sorted.get(i).body;
                pistils[at++] = sorted.get(i).pistils;
            }
        }
        return new Colours(bodies, pistils);
    }

    /**
     * {@code pieces} shared between groups of {@code counts} items by the largest remainder. Ties for a
     * leftover piece go to the earlier group, so a caller that lists the largest first gives it to the
     * larger share.
     */
    public static int[] share(int[] counts, int pieces) {
        long total = Arrays.stream(counts).asLongStream().sum();
        int[] out = new int[counts.length];
        if (total <= 0 || pieces <= 0) {
            return out;
        }
        long[] remainders = new long[counts.length];
        int given = 0;
        for (int i = 0; i < counts.length; i++) {
            long scaled = (long) counts[i] * pieces;
            out[i] = (int) (scaled / total);
            remainders[i] = scaled % total;
            given += out[i];
        }
        while (given < pieces) {
            int best = 0;
            for (int i = 1; i < counts.length; i++) {
                if (remainders[i] > remainders[best]) {
                    best = i;
                }
            }
            out[best]++;
            remainders[best] = -1;
            given++;
        }
        return out;
    }

    private static Group groupOf(ItemStackTemplate item, @Nullable HolderLookup.Provider registries) {
        if (item.typeHolder().value() == ModItems.SCHWAG) {
            return new Group("schwag", SCHWAG_COLOUR, SCHWAG_COLOUR, 0);
        }
        if (registries != null) {
            var strain = Strain.fromBuds(registries, item.typeHolder().value());
            if (strain.isPresent()) {
                return new Group("strain:" + strain.get().key().identifier(), BUD_GREEN,
                        strain.get().value().jarColourOrDefault(), 0);
            }
        }
        return new Group("item:" + BuiltInRegistries.ITEM.getKey(item.typeHolder().value()), BUD_GREEN, PLAIN_PISTILS, 0);
    }

    private record Group(String key, int body, int pistils, int count) {
        Group withCount(int count) {
            return new Group(key, body, pistils, count);
        }

        Group plus(Group other) {
            return new Group(key, body, pistils, count + other.count);
        }
    }
}
