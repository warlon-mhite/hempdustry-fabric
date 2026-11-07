package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.registry.DataRegistries;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.resources.ResourceLocation;

import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The dose is the number of slots holding buds, because crafting takes one item from each slot.
 *
 * <p>Until 2.0.3 the recipe counted the whole stack in a slot instead: three buds stacked in one
 * slot beside an empty bong packed a level-III bowl and used up one of them, and the Crafter would
 * repeat that all day.
 */
public final class PackingGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void oneBudPerSlotIsTheDose(GameTestHelper context) {
        Item buds = DataRegistries.of(context.getLevel()).lookupOrThrow(Strain.REGISTRY_KEY)
                .getOrThrow(ModStrains.INDICA).value().buds();
        PackingRecipe recipe = new PackingRecipe(new ResourceLocation("hempdustry", "packing"));

        int stacked = dose(recipe, new ItemStack(ModItems.BONG), new ItemStack(buds, 3));
        Line.equal(context, stacked, 1, "the dose from three buds stacked in one slot");

        int spread = dose(recipe, new ItemStack(ModItems.BONG), new ItemStack(buds), new ItemStack(buds));
        Line.equal(context, spread, 2, "the dose from a bud in each of two slots");
        context.succeed();
    }

    private static int dose(PackingRecipe recipe, ItemStack... grid) {
        List<ItemStack> stacks = new ArrayList<>(Arrays.asList(grid));
        while (stacks.size() < 9) {
            stacks.add(ItemStack.EMPTY);
        }
        ItemStack packed = recipe.assemble(CraftingInput.of(3, 3, stacks));
        return SmokingDeviceItem.contentsOf(packed).dose();
    }
}
