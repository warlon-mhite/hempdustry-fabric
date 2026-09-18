package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * A coloured bong is a bong in everything but its glass.
 *
 * <p>Seventeen items that are meant to behave identically to one other fail one at a time and
 * quietly: a packed Red Bong naming itself from {@code item.hempdustry.red_bong.packed}, a key no
 * locale has, shows the raw key rather than erroring; a colour whose recipe datagen skipped simply
 * cannot be crafted; a bong that repairs with the wrong glass looks like one that is merely fussy.
 */
public final class GlassBongGameTest {

    public static void everyColouredBongIsABong(TestContext context) {
        ServerWorld world = context.getWorld();
        PackingRecipe packing = new PackingRecipe(CraftingRecipeCategory.MISC);
        Item buds = world.getRegistryManager().getOrThrow(Strain.REGISTRY_KEY)
                .getOrThrow(ModStrains.INDICA).value().buds();

        context.assertEquals(ModItems.COLORED_BONGS.size(), 17,
                "there are not sixteen stained bongs and a tinted one");
        for (Item bong : ModItems.COLORED_BONGS) {
            String name = Registries.ITEM.getId(bong).getPath();

            // Dose 3 on purpose: it is the bong's alone, so a coloured bong read as any other
            // device would be refused here rather than passing at dose 1.
            CraftingRecipeInput grid = CraftingRecipeInput.create(2, 2, List.of(
                    new ItemStack(bong), new ItemStack(buds), new ItemStack(buds), new ItemStack(buds)));
            context.assertTrue(packing.matches(grid, world), name + " refused a dose-3 bowl");
            ItemStack packed = packing.craft(grid, world.getRegistryManager());
            context.assertTrue(packed.isOf(bong), name + " packed into a different item");
            context.assertEquals(packed.getOrDefault(ModComponents.CHARGES, 0), DeviceType.BONG.bowlSize(),
                    name + " did not load a bong's bowl");
            context.assertEquals(packed.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).dose(), 3,
                    name + " did not load three buds");

            String key = packed.getName().getContent() instanceof TranslatableTextContent t ? t.getKey() : "";
            context.assertTrue(key.equals("item.hempdustry.bong.packed"),
                    name + " packed names itself from " + key + ", which no locale defines");

            context.assertTrue(world.getServer().getRecipeManager().get(RegistryKey.of(RegistryKeys.RECIPE,
                    Identifier.of(Hempdustry.MOD_ID, name))).isPresent(), "nothing crafts " + name);

            String color = name.substring(0, name.length() - "_bong".length());
            Item glass = Registries.ITEM.get(Identifier.ofVanilla(
                    color.equals("tinted") ? "tinted_glass" : color + "_stained_glass"));
            context.assertTrue(new ItemStack(bong).canRepairWith(new ItemStack(glass)),
                    name + " does not repair with " + Registries.ITEM.getId(glass));
        }
        context.complete();
    }
}
