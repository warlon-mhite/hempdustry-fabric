package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * The dose is the number of slots holding buds, because crafting takes one item from each slot.
 *
 * <p>Until 2.0.3 the recipe counted the whole stack in a slot instead: three buds stacked in one
 * slot beside an empty bong packed a level-III bowl and used up one of them, and the Crafter would
 * repeat that all day.
 */
public final class PackingGameTest {

    public static void oneBudPerSlotIsTheDose(GameTestHelper context) {
        HolderLookup.Provider registries = context.getLevel().registryAccess();
        Item buds = registries.lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA).value().buds();
        PackingRecipe recipe = new PackingRecipe(CraftingBookCategory.MISC);

        int stacked = dose(recipe, registries, new ItemStack(ModItems.BONG), new ItemStack(buds, 3));
        context.assertValueEqual(1, stacked, "the dose from three buds stacked in one slot");

        int spread = dose(recipe, registries, new ItemStack(ModItems.BONG), new ItemStack(buds), new ItemStack(buds));
        context.assertValueEqual(2, spread, "the dose from a bud in each of two slots");
        context.succeed();
    }

    private static int dose(PackingRecipe recipe, HolderLookup.Provider registries, ItemStack... grid) {
        List<ItemStack> stacks = new ArrayList<>(List.of(grid));
        while (stacks.size() < 9) {
            stacks.add(ItemStack.EMPTY);
        }
        ItemStack packed = recipe.assemble(CraftingInput.of(3, 3, stacks), registries);
        return SmokingDeviceItem.contentsOf(packed).dose();
    }
}
