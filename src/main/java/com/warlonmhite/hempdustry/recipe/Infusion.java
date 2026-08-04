package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.CraftingRecipeInput;

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
 * conflating them was the bug. See CLAUDE.md §5b D13.
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
    public static ItemStack transfer(CraftingRecipeInput input, ItemStack result, int offset) {
        ItemStack butter = findButter(input);
        if (butter.isEmpty()) {
            return result;
        }
        int strength = butter.getOrDefault(ModComponents.STRENGTH, 1);
        Quality quality = butter.getOrDefault(ModComponents.QUALITY, Quality.ROUGH);

        result.set(ModComponents.POTENCY,
                EdibleEffects.applyOffset(EdibleEffects.tierFromStrength(strength), offset));
        result.set(ModComponents.QUALITY, quality);
        return result;
    }

    private static ItemStack findButter(CraftingRecipeInput input) {
        for (int slot = 0; slot < input.getSize(); slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (stack.isOf(ModItems.CANNABUTTER)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
