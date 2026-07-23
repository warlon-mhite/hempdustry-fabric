package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
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
 * Packs an empty pipe/bong with a strain's buds in the crafting grid, producing the matching packed
 * item. A special recipe (rather than a plain shapeless one) so it can copy the empty device's
 * components — durability, enchantments, repair cost — onto the packed result; a shapeless recipe
 * would emit a fresh, undamaged device and make it effectively unbreakable. One instance covers
 * every device × active strain by inspecting the grid.
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
        ItemStack packed = match.device.copyComponentsToNewStack(ModItems.packedDevice(device, match.strain), 1);
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

    /** Requires exactly one empty device + one active-strain bud and nothing else, else {@code null}. */
    private static Match find(CraftingRecipeInput input) {
        ItemStack device = ItemStack.EMPTY;
        Strain strain = null;
        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SmokingDeviceItem) {
                if (!device.isEmpty()) {
                    return null; // more than one device
                }
                device = stack;
            } else {
                Strain budStrain = Strain.fromBuds(stack.getItem());
                if (budStrain == null || strain != null) {
                    return null; // a foreign item, or more than one bud
                }
                strain = budStrain;
            }
        }
        return (!device.isEmpty() && strain != null) ? new Match(device, strain) : null;
    }

    private record Match(ItemStack device, Strain strain) {
    }
}
