package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The dose is the number of slots holding buds, because crafting takes one item from each slot.
 *
 * <p>Until 2.0.3 the recipe counted the whole stack in a slot instead: three buds stacked in one
 * slot beside an empty bong packed a level-III bowl and used up one of them, and the Crafter would
 * repeat that all day.
 */
public final class PackingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void oneBudPerSlotIsTheDose(TestContext context) {
        RegistryWrapper.WrapperLookup registries = context.getWorld().getRegistryManager();
        Item buds = context.getWorld().getRegistryManager().get(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA).buds();
        PackingRecipe recipe = new PackingRecipe(CraftingRecipeCategory.MISC);

        int stacked = dose(recipe, registries, new ItemStack(ModItems.BONG), new ItemStack(buds, 3));
        context.assertEquals(stacked, 1, "the dose from three buds stacked in one slot");

        int spread = dose(recipe, registries, new ItemStack(ModItems.BONG), new ItemStack(buds), new ItemStack(buds));
        context.assertEquals(spread, 2, "the dose from a bud in each of two slots");
        context.complete();
    }

    private static int dose(PackingRecipe recipe, RegistryWrapper.WrapperLookup registries, ItemStack... grid) {
        List<ItemStack> stacks = new ArrayList<>(List.of(grid));
        while (stacks.size() < 9) {
            stacks.add(ItemStack.EMPTY);
        }
        ItemStack packed = recipe.craft(CraftingRecipeInput.create(3, 3, stacks), registries);
        return SmokingDeviceItem.contentsOf(packed).dose();
    }
}
