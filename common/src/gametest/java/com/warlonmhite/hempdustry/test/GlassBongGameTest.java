package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * A coloured bong is a bong in everything but its glass.
 *
 * <p>Seventeen items that are meant to behave identically to one other fail one at a time and
 * quietly: a packed Red Bong naming itself from {@code item.hempdustry.red_bong.packed}, a key no
 * locale has, shows the raw key rather than erroring; a colour whose recipe datagen skipped simply
 * cannot be crafted; a bong that repairs with the wrong glass looks like one that is merely fussy.
 */
public final class GlassBongGameTest {

    public static void everyColouredBongIsABong(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        PackingRecipe packing = PackingRecipe.INSTANCE;
        Item buds = world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY)
                .getOrThrow(ModStrains.INDICA).value().buds();

        context.assertValueEqual(ModItems.COLORED_BONGS.size(), 17,
                "there are not sixteen stained bongs and a tinted one");
        for (Item bong : ModItems.COLORED_BONGS) {
            String name = BuiltInRegistries.ITEM.getKey(bong).getPath();

            // Dose 3 on purpose: it is the bong's alone, so a coloured bong read as any other
            // device would be refused here rather than passing at dose 1.
            CraftingInput grid = CraftingInput.of(2, 2, List.of(
                    new ItemStack(bong), new ItemStack(buds), new ItemStack(buds), new ItemStack(buds)));
            context.assertTrue(packing.matches(grid, world), name + " refused a dose-3 bowl");
            ItemStack packed = packing.assemble(grid, world.registryAccess());
            context.assertTrue(packed.is(bong), name + " packed into a different item");
            context.assertValueEqual(packed.getOrDefault(ModComponents.CHARGES, 0), DeviceType.BONG.builtInBowl().hits(),
                    name + " did not load a bong's bowl");
            context.assertValueEqual(packed.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).dose(), 3,
                    name + " did not load three buds");

            String key = packed.getHoverName().getContents() instanceof TranslatableContents t ? t.getKey() : "";
            context.assertTrue(key.equals("item.hempdustry.bong.packed"),
                    name + " packed names itself from " + key + ", which no locale defines");

            context.assertTrue(world.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name))).isPresent(), "nothing crafts " + name);

            String color = name.substring(0, name.length() - "_bong".length());
            Item glass = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(
                    color.equals("tinted") ? "tinted_glass" : color + "_stained_glass"));
            context.assertTrue(new ItemStack(bong).isValidRepairItem(new ItemStack(glass)),
                    name + " does not repair with " + BuiltInRegistries.ITEM.getKey(glass));
        }
        context.succeed();
    }
}
