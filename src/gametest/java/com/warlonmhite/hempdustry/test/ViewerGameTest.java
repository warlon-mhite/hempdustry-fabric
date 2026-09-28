package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;

import java.util.ArrayList;
import java.util.List;

/**
 * What a recipe viewer shows as packing is what the crafting grid does.
 *
 * <p>The packing rows are drawn from stacks the viewer model builds, not from the recipe, so
 * nothing else ties the two together: a moon rock drawn without its load, or a glass the bong slot
 * offers that the recipe would refuse, is a page that lies with every gate green. This packs every
 * row through the real {@link PackingRecipe}, once for each stack every slot offers.
 */
public final class ViewerGameTest {

    public static void everyPackingRowIsARealCraft(TestContext context) {
        ServerWorld world = context.getWorld();
        PackingRecipe packing = new PackingRecipe(CraftingRecipeCategory.MISC);
        List<ViewerRecipes.Packing> rows = ViewerRecipes.packing(world.getRegistryManager());
        context.assertFalse(rows.isEmpty(), "the viewers are shown no packing rows at all");

        int moonRockRows = 0;
        for (ViewerRecipes.Packing row : rows) {
            String id = row.id().toString();
            for (int slot = 0; slot < row.inputs().size(); slot++) {
                for (ItemStack offered : row.inputs().get(slot)) {
                    List<ItemStack> grid = new ArrayList<>();
                    for (int i = 0; i < 9; i++) {
                        grid.add(i >= row.inputs().size() ? ItemStack.EMPTY
                                : i == slot ? offered.copy() : row.inputs().get(i).getFirst().copy());
                    }
                    CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, grid);
                    context.assertTrue(packing.matches(input, world),
                            id + " is refused by the real recipe with " + offered.getName().getString());
                    ItemStack packed = packing.craft(input, world.getRegistryManager());
                    context.assertEquals(packed.get(ModComponents.SMOKE_CONTENTS),
                            row.output().get(ModComponents.SMOKE_CONTENTS),
                            id + " shows a different load from the one it crafts");
                    context.assertEquals(packed.get(ModComponents.CHARGES), row.output().get(ModComponents.CHARGES),
                            id + " shows a different bowl from the one it crafts");
                    if (slot == 0) {
                        context.assertTrue(packed.isOf(offered.getItem()),
                                id + " packs " + offered.getName().getString() + " into another item");
                    }
                }
            }
            if (row.inputs().stream().anyMatch(stacks -> stacks.getFirst().isOf(ModItems.MOON_ROCK))) {
                moonRockRows++;
            }
        }

        // One row per plant strain per coat: the coat is half of what a moon rock carries.
        long plants = Strain.all(world.getRegistryManager()).stream()
                .filter(strain -> strain.value().flower().isPresent()).count();
        context.assertEquals(moonRockRows, (int) plants * 3, "moon rock rows, one per plant and coat");
        context.assertTrue(rows.stream().anyMatch(row -> row.inputs().getFirst().size() == ModItems.bongs().size()),
                "no bong row offers every glass");
        context.assertFalse(rows.getFirst().output().getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY)
                        .entries().getFirst().strain().value().flower().isEmpty(),
                "the packing rows open on a strain that never grew on a plant");
        context.complete();
    }
}
