package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.ChemotypeBlocks;
import com.warlonmhite.hempdustry.block.custom.HempLeavesBlock;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.StrainPlantItem;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.StampedBlockItem;
import com.warlonmhite.hempdustry.item.custom.StampedItem;
import com.warlonmhite.hempdustry.strain.Chemotype;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * How a recipe carries a chemotype from what goes in to what comes out, so no route turns CBD matter
 * into THC matter. Every route that takes stamped matter in and gives stamped matter out goes
 * through here: the leaf block and the hash blocks both ways, spliffs and moon rocks, the furnace.
 *
 * <h2>One chemotype to a craft</h2>
 *
 * A crafting grid holding two chemotypes of stamped matter matches nothing, the way two strains in
 * one bowl pack nothing: nine leaves make a block of one kind, and a spliff burns one material. Only
 * the Infuser, the sieve and bhang mix chemotypes, by their dosage ({@link Chemotype#dosage}). A bud
 * in the grid is not stamped matter: it is its strain, and the result names its strain already.
 *
 * <h2>Bands mix, marks do not</h2>
 *
 * Buds carry their plant's band ({@link PotencyBand}): a spliff or a moon rock rolled from buds of
 * different bands keeps the sum of their offsets, which is what their THC averages to. Leaves carry a
 * high mark, and a block holds one: a grid of marked and plain leaves matches nothing, and the result
 * takes the mark they share — the block of nine marked leaves, the nine leaves out of it.
 */
public final class Stamping {
    private Stamping() {
    }

    /** The stamp a crafting ingredient brings, or empty for anything that carries none. */
    public static Optional<Chemotype> stampOf(ItemStack stack) {
        if (stack.getItem() instanceof StampedItem) {
            return Optional.of(Chemotype.of(stack));
        }
        if (stack.getItem() instanceof StampedBlockItem) {
            return Optional.of(ChemotypeBlocks.ofBlockItem(stack));
        }
        return Optional.empty();
    }

    /**
     * The one chemotype the stamped matter in {@code input} shares: empty when there is none in it,
     * and {@code null} when two disagree, which refuses the craft.
     */
    public static Optional<Chemotype> shared(CraftingInput input) {
        Chemotype found = null;
        for (int slot = 0; slot < input.size(); slot++) {
            Optional<Chemotype> stamp = stampOf(input.getItem(slot));
            if (stamp.isEmpty()) {
                continue;
            }
            if (found != null && found != stamp.get()) {
                return null;
            }
            found = stamp.get();
        }
        return Optional.ofNullable(found);
    }

    /** The high mark a leaf or a leaf block brings (mid when unmarked); empty for anything else. */
    public static Optional<PotencyBand> markOf(ItemStack stack) {
        if (stack.is(ModItems.HEMP_LEAF)) {
            return Optional.of(PotencyBand.of(stack));
        }
        if (stack.is(ModBlocks.HEMP_LEAVES.asItem())) {
            return Optional.of(HempLeavesBlock.markOfBlockItem(stack));
        }
        return Optional.empty();
    }

    /**
     * The one mark the leaves and leaf blocks in {@code input} share: empty when there are none, and
     * {@code null} when two disagree, which refuses the craft — a block holds one mark.
     */
    public static Optional<PotencyBand> sharedMark(CraftingInput input) {
        PotencyBand found = null;
        for (int slot = 0; slot < input.size(); slot++) {
            Optional<PotencyBand> mark = markOf(input.getItem(slot));
            if (mark.isEmpty()) {
                continue;
            }
            if (found != null && found != mark.get()) {
                return null;
            }
            found = mark.get();
        }
        return Optional.ofNullable(found);
    }

    /**
     * The sum of the band offsets of the buds in {@code input} (one bud per occupied slot, as crafting
     * takes), empty when there are none: what a spliff or a moon rock rolled from them carries, so buds
     * of different bands come out at their average.
     */
    public static Optional<Integer> budPotency(CraftingInput input) {
        int sum = 0;
        boolean any = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof StrainPlantItem.Buds) {
                sum += PotencyBand.of(stack).offset();
                any = true;
            }
        }
        return any ? Optional.of(sum) : Optional.empty();
    }

    /** Whether the stamped matter in {@code input} is all of one chemotype, and its leaves of one mark. */
    public static boolean agrees(CraftingInput input) {
        return shared(input) != null && sharedMark(input) != null;
    }

    /**
     * The result of a craft over {@code input}, stamped with the chemotype its stamped matter shares,
     * marked like its leaves, and, for a loaded spliff or moon rock, carrying its buds' bands.
     */
    public static ItemStack carry(CraftingInput input, ItemStack result) {
        Optional<Chemotype> chemotype = shared(input);
        if (chemotype != null && chemotype.isPresent()) {
            result = apply(result, chemotype.get());
        }
        Optional<PotencyBand> mark = sharedMark(input);
        if (mark != null && mark.isPresent()) {
            if (result.is(ModItems.HEMP_LEAF)) {
                PotencyBand.stamp(result, mark.get().marksLeaf() ? PotencyBand.HIGH : PotencyBand.MID);
            } else if (result.is(ModBlocks.HEMP_LEAVES.asItem())) {
                HempLeavesBlock.markBlockItem(result, mark.get());
            }
        }
        Optional<Integer> potency = budPotency(input);
        SmokeContents contents = result.get(ModComponents.SMOKE_CONTENTS);
        if (potency.isPresent() && potency.get() != 0 && contents != null && !contents.isEmpty()) {
            result.set(ModComponents.SMOKE_CONTENTS, contents.withPotency(potency.get()));
        }
        return result;
    }

    /**
     * Stamps {@code result} with {@code chemotype}: the item itself when it is stamped matter, the
     * block state it places with when it is a stamped block, and, for a loaded spliff or moon rock,
     * every entry that is not a plant's buds (a plant's buds are their strain).
     */
    public static ItemStack apply(ItemStack result, Chemotype chemotype) {
        if (result.getItem() instanceof StampedItem) {
            return Chemotype.stamp(result, chemotype);
        }
        if (result.getItem() instanceof StampedBlockItem) {
            return ChemotypeBlocks.stampBlockItem(result, chemotype);
        }
        SmokeContents contents = result.get(ModComponents.SMOKE_CONTENTS);
        if (contents != null && !contents.isEmpty()) {
            List<SmokeContents.Entry> stamped = new ArrayList<>(contents.entries().size());
            for (SmokeContents.Entry entry : contents.entries()) {
                stamped.add(entry.strain().value().wildFlower().isPresent()
                        ? entry
                        : new SmokeContents.Entry(entry.strain(), entry.count(), chemotype));
            }
            result.set(ModComponents.SMOKE_CONTENTS, new SmokeContents(stamped));
        }
        return result;
    }

    /**
     * What a single piece of matter going into a machine is: a bud its strain, anything else its stamp.
     */
    public static Chemotype matter(ItemStack stack, HolderLookup.Provider registries) {
        return Chemotype.ofMatter(stack, registries);
    }
}
