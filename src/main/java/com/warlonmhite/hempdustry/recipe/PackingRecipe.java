package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.item.custom.Strain;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Packs an empty pipe/bong with buds in the crafting grid. The number of buds is the <b>dose</b>,
 * which becomes the effect level of every hit from that bowl — 1 bud is level I, up to the device's
 * {@link DeviceType#maxDose()}. See CLAUDE.md §5b D10.
 *
 * <h2>Why this stays a special recipe</h2>
 *
 * A plain shapeless recipe's result is a fixed {@link ItemStack}, so it cannot carry the device's
 * durability, enchantments and repair cost across — it would hand back a pristine device and make
 * the thing effectively unbreakable. It also cannot vary its output by how many buds were supplied.
 * One instance covers every device × strain × dose by inspecting the grid.
 *
 * <p>The cost of staying special is that neither the recipe book nor JEI/EMI can see this at all,
 * which is why a recipe-viewer plugin is a hard requirement rather than a nicety — CLAUDE.md §5b D11.
 */
public class PackingRecipe extends SpecialCraftingRecipe {
    public PackingRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return find(input) != null;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        Match match = find(input);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        DeviceType device = ((SmokingDeviceItem) match.device.getItem()).device();
        // Copying the stack rather than building a fresh one is what carries durability and
        // enchantments through. Packing is now just two components being set on the same item.
        ItemStack packed = match.device.copyWithCount(1);
        packed.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(match.strain, match.dose));
        packed.set(ModComponents.CHARGES, device.bowlSize());
        return packed;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PACKING;
    }

    /**
     * Requires exactly one <em>empty</em> device plus 1..maxDose buds of a single active strain and
     * nothing else, else {@code null}.
     *
     * <p>An already-packed device is rejected deliberately: allowing it would silently discard the
     * bowl already in there along with whatever charges were left on it.
     */
    private static Match find(CraftingRecipeInput input) {
        ItemStack device = ItemStack.EMPTY;
        Strain strain = null;
        int dose = 0;
        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SmokingDeviceItem) {
                if (!device.isEmpty() || !SmokingDeviceItem.contentsOf(stack).isEmpty()) {
                    return null; // more than one device, or one that is already packed
                }
                device = stack;
            } else {
                Strain budStrain = Strain.fromBuds(stack.getItem());
                if (budStrain == null || (strain != null && strain != budStrain)) {
                    return null; // a foreign item, or a second strain (no mixing yet)
                }
                strain = budStrain;
                dose += stack.getCount();
            }
        }
        if (device.isEmpty() || strain == null) {
            return null;
        }
        DeviceType type = ((SmokingDeviceItem) device.getItem()).device();
        return dose >= 1 && dose <= type.maxDose() ? new Match(device, strain, dose) : null;
    }

    private record Match(ItemStack device, Strain strain, int dose) {
    }
}
