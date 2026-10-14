package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/**
 * The creative tab is built without throwing — <b>and this test exists because it once did.</b>
 *
 * <h2>The bug, and why nothing caught it</h2>
 *
 * On 2026-09-09 an edit to {@code ModItemGroups} added
 * {@code entries.add(WASHED_DECARBOXYLATED_HEMP)} without removing the existing one. Vanilla's
 * {@code CreativeModeTab$ItemDisplayBuilder.accept} refuses that outright —
 * <i>"Accidentally adding the same item stack twice"</i>, an {@code IllegalStateException} — so
 * <b>opening the creative inventory crashed the client, in every world.</b>
 *
 * <p>It reached a play-test through a full green board: {@code build}, {@code runDatagen},
 * {@code recipe_collisions.py} and sixteen game tests all passed. They passed because
 * <b>{@code runGametest} boots a dedicated server, and a dedicated server never populates a creative
 * tab</b> — {@code CreativeModeTabs.tryRebuildTabContents} runs from {@code CreativeModeInventoryScreen},
 * on the client, when a player presses E. The mod's largest and most-edited lambda had no coverage at all.
 *
 * <p><b>It did not have to be that way, and that is the actual lesson.</b>
 * {@code net.minecraft.world.item.CreativeModeTabs} and {@code CreativeModeTab#buildContents} are
 * <em>common</em> code, not client code: a headless server can build the tab perfectly well: it simply had
 * never been asked to. "It only happens on a client" was an assumption, not a fact — and the way to check an
 * assumption like that is {@code javap} against the mapped jar, which takes ten seconds.
 *
 * <h2>What this guards, beyond the duplicate</h2>
 *
 * Everything the entries lambda does, which is more than it looks: it reads the strain registry,
 * builds loaded spliffs, devices and moon rocks from it, and calls
 * {@code ModItems.allSmokeables} / {@code allDosed} / {@code moonRocks}. <b>Any</b> exception in any
 * of that is a crash on pressing E, and all of it now runs headless.
 */
public final class CreativeTabGameTest {

    public static void creativeTabPopulates(GameTestHelper context) {
        ServerLevel world = context.getLevel();

        // Exactly what CreativeModeInventoryScreen does on opening, minus the screen. A duplicate stack
        // throws IllegalStateException from inside here, which fails the test as an error rather
        // than as an assertion -- which is the right shape: this is "does it work at all".
        CreativeModeTab.ItemDisplayParameters display = new CreativeModeTab.ItemDisplayParameters(
                world.enabledFeatures(), true, world.registryAccess());
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.buildContents(display);

        List<ItemStack> all = new ArrayList<>(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayItems());
        all.addAll(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabDisplayItems());
        // A tab that quietly built nothing would pass the throw test and fail the player, so the
        // count is asserted too rather than assumed.
        context.assertTrue(all.size() > 50,
                "the creative tab built only " + all.size() + " stacks — it did not populate");

        // Every item of ours is in the tab. "Add every new item to ModItemGroups" is a rule, and a rule
        // with no check is a suggestion: until 2026-10-08 this named seven items by hand, so an item
        // added since was checked by nothing.
        Map<String, String> missing = new TreeMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (Sweep.ours(id) && all.stream().noneMatch(stack -> stack.is(item))) {
                missing.put(id.toString(), "is in no stack the tab or creative search shows");
            }
        }
        Sweep.report(context, "Every item of ours is in the creative tab", missing, Map.of());

        // Everything the tab shows can be found by searching for it. PARENT_TAB_ONLY keeps a stack
        // out of the search tab, and is meant for a showcase whose full run is listed search-only;
        // the moon rocks had no such run, so search never found one.
        for (ItemStack shown : ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayItems()) {
            boolean searchable = false;
            for (ItemStack stack : ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabDisplayItems()) {
                if (stack.is(shown.getItem())) {
                    searchable = true;
                    break;
                }
            }
            context.assertTrue(searchable, shown.getItem() + " is in the creative tab but creative search never finds it");
        }

        // Every loaded spliff and moon rock a recipe makes can be found by searching for it, down to
        // its contents. The tab builds those stacks from the registry and the recipes build their
        // own, so the two drifted: the schwag spliffs, the hash spliffs and two of the three moon
        // rock coats were craftable and in no list.
        for (RecipeHolder<?> entry : world.getServer().getRecipeManager().getRecipes()) {
            for (RecipeDisplay shown : entry.value().display()) {
                if (!(shown.result() instanceof SlotDisplay.ItemStackSlotDisplay(ItemStack made))
                        || !(made.is(ModItems.SPLIFF) || made.is(ModItems.MOON_ROCK))) {
                    continue;
                }
                boolean listed = false;
                for (ItemStack stack : ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabDisplayItems()) {
                    if (ItemStack.isSameItemSameComponents(stack, made)) {
                        listed = true;
                        break;
                    }
                }
                context.assertTrue(listed, entry.id().identifier() + " makes a stack creative search never finds");
            }
        }
        context.succeed();
    }
}
