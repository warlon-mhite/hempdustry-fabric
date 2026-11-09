package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.strain.Chemotype;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * Carries cannabutter's two axes onto whatever is baked with it.
 *
 * <p><b>Why this cannot be a plain recipe.</b> A vanilla crafting recipe's result is a fixed
 * {@link ItemStack} — there is no hook for reading an ingredient's components and writing them onto
 * the output. Only a recipe class that overrides {@code craft} can see the grid, which is why
 * {@link InfusedShapedRecipe} and {@link InfusedShapelessRecipe} exist at all.
 *
 * <h2>Strength divides into tiers; the offset is the edible's own identity</h2>
 *
 * The butter's raw {@code strength} (1–24 hemp) collapses to a potency tier I–IV, and the edible then
 * applies a single offset, clamped. <b>Yield gets no vote.</b> An earlier design divided strength by
 * the recipe's yield, which rounded to zero on weak batches and made toast strong purely because it
 * came three to a butter — the food economy and the potency ladder are different questions and
 * conflating them was the bug.
 *
 * <p>Quality copies across unchanged: it is a grade, not a quantity.
 */
public final class Infusion {
    private Infusion() {
    }

    /**
     * Reads the cannabutter out of {@code input} and writes the resulting potency and quality onto
     * {@code result}. Returns the result untouched if there is no butter in the grid, which keeps a
     * malformed match harmless rather than throwing.
     */
    public static ItemStack transfer(CraftingInput input, ItemStack result, int offset) {
        ItemStack butter = findButter(input);
        if (butter.isEmpty()) {
            return result;
        }
        int strength = butter.getOrDefault(ModComponents.STRENGTH, 1);
        int cbd = butter.getOrDefault(ModComponents.CBD_STRENGTH, 0);
        Quality quality = butter.getOrDefault(ModComponents.QUALITY, Quality.ROUGH);

        // The THC sum gives the potency and the CBD sum the CBD tier, each through the same four
        // tiers and the edible's own offset. CBD butter has strength 0, so its edibles carry no
        // potency at all: no high, no munchies, only what answers to CBD.
        if (strength > 0) {
            result.set(ModComponents.POTENCY,
                    EdibleEffects.applyOffset(EdibleEffects.tierFromStrength(strength), offset));
        } else {
            result.remove(ModComponents.POTENCY);
        }
        if (cbd > 0) {
            result.set(ModComponents.CBD_POTENCY,
                    EdibleEffects.applyOffset(EdibleEffects.tierFromStrength(cbd), offset));
        }
        result.set(ModComponents.QUALITY, quality);
        return Chemotype.stamp(result, Chemotype.of(butter));
    }

    /**
     * Bhang's dose: the dosage of its two decarboxylated hemp decides its chemotype (two THC-dominant
     * or one with a balanced one are THC-dominant; one of each end, or two balanced, are balanced; the
     * rest CBD-dominant). The THC is fixed at tier I, as it always was, and CBD bhang takes tier I of
     * the CBD's length. Rough, like every bhang.
     */
    public static ItemStack bhang(CraftingInput input, ItemStack result) {
        int[] counts = new int[Chemotype.values().length];
        boolean any = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(ModItems.DECARBOXYLATED_HEMP) || stack.is(ModItems.WASHED_DECARBOXYLATED_HEMP)) {
                counts[Chemotype.of(stack).ordinal()]++;
                any = true;
            }
        }
        if (!any || !result.has(ModComponents.POTENCY)) {
            return result;
        }
        Chemotype chemotype = Chemotype.dosage(counts);
        if (!chemotype.hasThc()) {
            result.remove(ModComponents.POTENCY);
        }
        if (chemotype.cbdHalves() > 0) {
            result.set(ModComponents.CBD_POTENCY, 1);
        }
        return Chemotype.stamp(result, chemotype);
    }

    /**
     * Whether bhang takes the hemp in {@code input}: not the two chemotypes that make neither THC nor
     * CBD. The oven and the Infuser already turn them away, so this third door into an edible must too
     * (they reach a grid only through a command or a datapack).
     */
    public static boolean bhangTakes(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if ((stack.is(ModItems.DECARBOXYLATED_HEMP) || stack.is(ModItems.WASHED_DECARBOXYLATED_HEMP))
                    && !Chemotype.of(stack).infusable()) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack findButter(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(ModItems.CANNABUTTER)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
