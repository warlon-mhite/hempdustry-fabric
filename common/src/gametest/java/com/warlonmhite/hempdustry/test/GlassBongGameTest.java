package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.registry.DataRegistries;
import com.google.common.collect.ImmutableList;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.List;
import net.minecraft.core.Registry;
import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A coloured bong is a bong in everything but its glass.
 *
 * <p>Sixteen items that are meant to behave identically to one other fail one at a time and
 * quietly: a packed Red Bong naming itself from {@code item.hempdustry.red_bong.packed}, a key no
 * locale has, shows the raw key rather than erroring; a colour whose recipe datagen skipped simply
 * cannot be crafted; a bong that repairs with the wrong glass looks like one that is merely fussy.
 */
public final class GlassBongGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyColouredBongIsABong(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        PackingRecipe packing = new PackingRecipe(new ResourceLocation("hempdustry", "packing"));
        Item buds = DataRegistries.of(world).lookupOrThrow(Strain.REGISTRY_KEY)
                .getOrThrow(ModStrains.INDICA).value().buds();

        // The newer lines also have a tinted bong; 1.16.5 has no tinted glass (1.17), so only the
        // sixteen stained ones are here.
        Line.equal(context, 16, ModItems.COLORED_BONGS.size(),
                "there are not sixteen stained bongs");
        for (Item bong : ModItems.COLORED_BONGS) {
            String name = Registry.ITEM.getKey(bong).getPath();

            // Dose 3 on purpose: it is the bong's alone, so a coloured bong read as any other
            // device would be refused here rather than passing at dose 1.
            CraftingInput grid = CraftingInput.of(2, 2, ImmutableList.of(
                    new ItemStack(bong), new ItemStack(buds), new ItemStack(buds), new ItemStack(buds)));
            context.assertTrue(packing.matches(grid, world), name + " refused a dose-3 bowl");
            ItemStack packed = packing.assemble(grid);
            context.assertTrue(packed.getItem() == bong, name + " packed into a different item");
            Line.equal(context, DeviceType.BONG.builtInBowl().hits(), ModComponents.CHARGES.getOrDefault(packed, 0),
                    name + " did not load a bong's bowl");
            Line.equal(context, 3, ModComponents.SMOKE_CONTENTS.getOrDefault(packed, SmokeContents.EMPTY).dose(),
                    name + " did not load three buds");

            String key = packed.getHoverName() instanceof TranslatableComponent t ? t.getKey() : "";
            context.assertTrue(key.equals("item.hempdustry.bong.packed"),
                    name + " packed names itself from " + key + ", which no locale defines");

            context.assertTrue(world.getServer().getRecipeManager().byKey(
                    new ResourceLocation(Hempdustry.MOD_ID, name)).isPresent(), "nothing crafts " + name);

            String color = name.substring(0, name.length() - "_bong".length());
            Item glass = Registry.ITEM.get(new ResourceLocation(color + "_stained_glass"));
            context.assertTrue(bong.isValidRepairItem(new ItemStack(bong), new ItemStack(glass)),
                    name + " does not repair with " + Registry.ITEM.getKey(glass));
        }
        context.succeed();
    }
}
