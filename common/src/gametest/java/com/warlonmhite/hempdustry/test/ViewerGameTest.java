package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * What a recipe viewer shows as packing is what the crafting grid does.
 *
 * <p>The packing rows are drawn from stacks the viewer model builds, not from the recipe, so
 * nothing else ties the two together: a moon rock drawn without its load, or a glass the bong slot
 * offers that the recipe would refuse, is a page that lies with every gate green. This packs every
 * row through the real {@link PackingRecipe}, once for each stack every slot offers.
 */
public final class ViewerGameTest {

    public static void everyPackingRowIsARealCraft(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        PackingRecipe packing = PackingRecipe.INSTANCE;
        List<ViewerRecipes.Packing> rows = ViewerRecipes.packing(world.registryAccess());
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
                    CraftingInput input = CraftingInput.of(3, 3, grid);
                    context.assertTrue(packing.matches(input, world),
                            id + " is refused by the real recipe with " + offered.getHoverName().getString());
                    ItemStack packed = packing.assemble(input, world.registryAccess());
                    context.assertValueEqual(packed.get(ModComponents.SMOKE_CONTENTS),
                            row.output().get(ModComponents.SMOKE_CONTENTS),
                            id + " shows a different load from the one it crafts");
                    context.assertValueEqual(packed.get(ModComponents.CHARGES), row.output().get(ModComponents.CHARGES),
                            id + " shows a different bowl from the one it crafts");
                    if (slot == 0) {
                        context.assertTrue(packed.is(offered.getItem()),
                                id + " packs " + offered.getHoverName().getString() + " into another item");
                    }
                }
            }
            if (row.inputs().stream().anyMatch(stacks -> stacks.getFirst().is(ModItems.MOON_ROCK))) {
                moonRockRows++;
            }
        }

        // One row per plant strain per coat: the coat is half of what a moon rock carries.
        long plants = Strain.all(world.registryAccess()).stream()
                .filter(strain -> strain.value().wildFlower().isPresent()).count();
        context.assertValueEqual(moonRockRows, (int) plants * 3, "moon rock rows, one per plant and coat");
        context.assertTrue(rows.stream().anyMatch(row -> row.inputs().getFirst().size() == ModItems.bongs().size()),
                "no bong row offers every glass");
        context.assertFalse(rows.getFirst().output().getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY)
                        .entries().getFirst().strain().value().wildFlower().isEmpty(),
                "the packing rows open on a strain that never grew on a plant");
        context.succeed();
    }

    /**
     * Every viewer page's notes fit the room its category has for them. Both viewers size a category once, for
     * every page in it, so a note past the room is drawn over the next page: the resin line on the sifting,
     * wash and rubbing pages did that until their room was counted here.
     */
    public static void everyViewerNoteFitsItsPage(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Map<Identifier, List<ViewerRecipes.Entry>> pages = Map.of(
                ViewerRecipes.DECARBOXYLATING, ViewerRecipes.decarboxylating(world),
                ViewerRecipes.INFUSING, ViewerRecipes.infusing(world),
                ViewerRecipes.PRESSING, ViewerRecipes.pressing(world),
                ViewerRecipes.CAULDRON, ViewerRecipes.cauldron(),
                ViewerRecipes.SIFTING, ViewerRecipes.sifting(world),
                ViewerRecipes.ICE_O_LATOR, ViewerRecipes.iceOLator(world),
                ViewerRecipes.WORLD, ViewerRecipes.world(world));
        List<String> over = new ArrayList<>();
        int seen = 0;
        for (Map.Entry<Identifier, List<ViewerRecipes.Entry>> page : pages.entrySet()) {
            int room = ViewerRecipes.noteLines(page.getKey());
            for (ViewerRecipes.Entry entry : page.getValue()) {
                seen++;
                if (entry.notes().size() > room) {
                    over.add(entry.id() + ": " + entry.notes().size() + " lines, room for " + room);
                }
            }
        }
        context.assertTrue(seen >= 20, "only " + seen + " viewer rows to measure");
        context.assertTrue(over.isEmpty(), "viewer notes past their page's room: " + over);
        context.succeed();
    }
}
