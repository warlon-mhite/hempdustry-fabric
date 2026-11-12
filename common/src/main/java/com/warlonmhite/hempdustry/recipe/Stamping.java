package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.ChemotypeBlocks;
import com.warlonmhite.hempdustry.block.custom.HempLeavesBlock;
import com.warlonmhite.hempdustry.block.custom.TierBlocks;
import com.warlonmhite.hempdustry.strain.BudTier;
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
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * How a recipe carries a chemotype from what goes in to what comes out, so no route turns CBD matter
 * into THC matter. Every route that takes stamped matter in and gives stamped matter out goes
 * through here: the leaf block and the hash blocks both ways, spliffs and moon rocks, the furnace.
 *
 * <h2>One chemotype of stamped matter to a craft</h2>
 *
 * A crafting grid holding two chemotypes of stamped matter matches nothing, the way two strains in
 * one bowl pack nothing: nine leaves make a block of one kind, and a spliff burns one material. Only
 * the Infuser, the sieve and bhang mix chemotypes, by their dosage ({@link Chemotype#dosage}). A bud
 * in the grid is not stamped matter: it is its strain, and the result names its strain already. So a
 * bud may meet stamped matter of another chemotype (a Beldía bud with THC hashish in a hash spliff, a
 * Purple Kush bud with CBD kief in a moon rock): the load then holds two entries, each keeping its
 * own chemotype, and the bowl weighs each as it is.
 *
 * <h2>Bands mix, marks do not</h2>
 *
 * Buds carry their plant's band ({@link PotencyBand}): a spliff or a moon rock rolled from buds of
 * different bands keeps the sum of their offsets, which is what their THC averages to. Leaves carry a
 * high mark, and a block holds one: a grid of marked and plain leaves matches nothing, and the result
 * takes the mark they share — the block of nine marked leaves, the nine leaves out of it.
 *
 * <h2>Tiers: one to a block, the weakest to a smoke, each entry its own</h2>
 *
 * Buds carry their harvest's tier and the sifted hash family its batch's ({@link BudTier}). Nine pieces
 * of hash make a bar of one tier, so a grid of hash of two tiers with no bud in it matches nothing. A
 * spliff or a moon rock keeps two tiers apart: its plant entry takes the weakest of its buds, its hash
 * entry the weakest of its hash, and each tier moves only its own entry's effects. Charas carries none
 * and counts as Dank.
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
     *
     * <p><b>A moon rock counts its bud's band once.</b> Its other two units are hash and rosin, which
     * carry no band, so the coat never scales it: concentrate is most of the rock's THC. Only a bud
     * worth more than one unit ({@code dose_per_item}, a datapack's) carries its band on each unit,
     * twice the mass at the same share ({@link #budOffset}).
     */
    public static Optional<Integer> budPotency(CraftingInput input) {
        HolderLookup.Provider registries = PackingRecipe.serverRegistries();
        int sum = 0;
        boolean any = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof StrainPlantItem.Buds) {
                sum += budOffset(stack, registries);
                any = true;
            }
        }
        return any ? Optional.of(sum) : Optional.empty();
    }

    /**
     * One bud's band offset as a load counts it: the band's offset on each unit the bud is worth
     * ({@code dose_per_item}, one for every strain the mod ships), so a load's sum stays exactly what
     * its THC averages to. With no registries to ask (nothing is crafting), one unit.
     */
    public static int budOffset(ItemStack bud, @Nullable HolderLookup.Provider registries) {
        int units = registries == null ? 1 : Strain.fromBuds(registries, bud.getItem())
                .map(strain -> Math.max(1, strain.value().dosePerItem()))
                .orElse(1);
        return PotencyBand.of(bud).offset() * units;
    }

    /** Whether the stamped matter in {@code input} is all of one chemotype, its leaves of one mark, and its hash of one tier. */
    public static boolean agrees(CraftingInput input) {
        return shared(input) != null && sharedMark(input) != null && (hasBud(input) || resinTiers(input) != null);
    }

    /** The tier a piece of tiered hash, or the item of a bar of it, brings; empty for anything else. */
    public static Optional<BudTier> resinTierOf(ItemStack stack) {
        if (stack.getItem() instanceof StampedItem.Resin) {
            return Optional.of(BudTier.of(stack));
        }
        if (TierBlocks.holdsTier(stack)) {
            return Optional.of(TierBlocks.ofBlockItem(stack));
        }
        return Optional.empty();
    }

    /**
     * The one tier the hash in {@code input} shares: empty when there is none, and {@code null} when two
     * disagree — a bar holds one tier.
     */
    public static Optional<BudTier> resinTiers(CraftingInput input) {
        BudTier found = null;
        for (int slot = 0; slot < input.size(); slot++) {
            Optional<BudTier> tier = resinTierOf(input.getItem(slot));
            if (tier.isEmpty()) {
                continue;
            }
            if (found != null && found != tier.get()) {
                return null;
            }
            found = tier.get();
        }
        return Optional.ofNullable(found);
    }

    /** The weakest tier among the hash in {@code input}, empty when there is none. */
    public static Optional<BudTier> weakestResinTier(CraftingInput input) {
        BudTier weakest = null;
        for (int slot = 0; slot < input.size(); slot++) {
            Optional<BudTier> tier = resinTierOf(input.getItem(slot));
            if (tier.isPresent()) {
                weakest = weakest == null ? tier.get() : BudTier.weaker(weakest, tier.get());
            }
        }
        return Optional.ofNullable(weakest);
    }

    /** The weakest tier among the buds in {@code input}, empty when there are none. */
    public static Optional<BudTier> weakestBudTier(CraftingInput input) {
        BudTier weakest = null;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof StrainPlantItem.Buds) {
                BudTier tier = BudTier.of(stack);
                weakest = weakest == null ? tier : BudTier.weaker(weakest, tier);
            }
        }
        return Optional.ofNullable(weakest);
    }

    private static boolean hasBud(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            if (input.getItem(slot).getItem() instanceof StrainPlantItem.Buds) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes {@code tier} on {@code result} if it carries one: a piece of tiered hash, or the item of a
     * bar (its block state). Anything else is returned as it was.
     */
    public static ItemStack applyTier(ItemStack result, BudTier tier) {
        if (result.getItem() instanceof StampedItem.Resin) {
            return BudTier.stamp(result, tier);
        }
        if (TierBlocks.holdsTier(result)) {
            return TierBlocks.stampBlockItem(result, tier);
        }
        return result;
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
        Optional<BudTier> resinTier = weakestResinTier(input);
        if (resinTier.isPresent()) {
            result = applyTier(result, resinTier.get());
        }
        Optional<Integer> potency = budPotency(input);
        SmokeContents contents = result.get(ModComponents.SMOKE_CONTENTS);
        if (potency.isPresent() && potency.get() != 0 && contents != null && !contents.isEmpty()) {
            contents = contents.withPotency(potency.get());
            result.set(ModComponents.SMOKE_CONTENTS, contents);
        }
        Optional<BudTier> budTier = weakestBudTier(input);
        if (contents != null && !contents.isEmpty() && (budTier.isPresent() || resinTier.isPresent())) {
            result.set(ModComponents.SMOKE_CONTENTS, contents.withTiers(budTier, resinTier));
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
                        : entry.withStamp(chemotype));
            }
            result.set(ModComponents.SMOKE_CONTENTS, new SmokeContents(stamped));
        }
        return result;
    }

    /**
     * What a single piece of matter going into a machine is (a bud its strain, anything else its
     * stamp), asked with the running server's registries since 26.1 hands a recipe none. With no
     * server, a bud reads as its stamp, THC-dominant: nothing is crafting then.
     */
    public static Chemotype matter(ItemStack stack) {
        HolderLookup.Provider registries = PackingRecipe.serverRegistries();
        return registries == null ? Chemotype.of(stack) : Chemotype.ofMatter(stack, registries);
    }
}
