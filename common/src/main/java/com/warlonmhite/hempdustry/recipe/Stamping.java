package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.block.custom.ChemotypeBlocks;
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

    /** Whether the stamped matter in {@code input} is all of one chemotype. */
    public static boolean agrees(CraftingInput input) {
        return shared(input) != null;
    }

    /** The result of a craft over {@code input}, stamped with the chemotype its stamped matter shares. */
    public static ItemStack carry(CraftingInput input, ItemStack result) {
        Optional<Chemotype> chemotype = shared(input);
        return chemotype == null || chemotype.isEmpty() ? result : apply(result, chemotype.get());
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
     * What a single piece of matter going into a machine is (a bud its strain, anything else its
     * stamp), asked with the running server's registries since 26.1 hands a recipe none. With no
     * server, a bud reads as its stamp, THC-dominant: nothing is crafting then.
     */
    public static Chemotype matter(ItemStack stack) {
        HolderLookup.Provider registries = PackingRecipe.serverRegistries();
        return registries == null ? Chemotype.of(stack) : Chemotype.ofMatter(stack, registries);
    }
}
