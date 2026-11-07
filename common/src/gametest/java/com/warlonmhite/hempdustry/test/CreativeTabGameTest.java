package com.warlonmhite.hempdustry.test;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.item.crafting.Recipe;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.TreeMap;

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
 * <h2>On 1.16.5</h2>
 *
 * A 1.16.5 tab is built on the client only ({@code fillItemList} is client-only), and the search tab
 * asks every item for its stacks; {@link CreativeTab} builds both on the test server through the mod's
 * own overrides. A duplicate stack is no crash there, only a stack shown twice, so the duplicate is
 * asserted outright: the same entries crash the newer lines' tab.
 *
 * <h2>What this guards, beyond the duplicate</h2>
 *
 * Everything the entries lambda does, which is more than it looks: it reads the strain registry,
 * builds loaded spliffs, devices and moon rocks from it, and calls
 * {@code ModItems.allSmokeables} / {@code allDosed} / {@code moonRocks}. <b>Any</b> exception in any
 * of that is a crash on pressing E, and all of it now runs headless.
 */
public final class CreativeTabGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void creativeTabPopulates(GameTestHelper context) {
        ServerLevel world = context.getLevel();

        // Exactly what the creative screen asks of the tab on opening, minus the screen. Anything the
        // entries throw fails the test as an error rather than as an assertion -- which is the right
        // shape: this is "does it work at all".
        List<ItemStack> shownInTab = CreativeTab.tab();
        List<ItemStack> searched = CreativeTab.search();

        // The newer lines' tab throws on the same stack added to it twice; 1.16.5's shows it twice.
        for (int i = 0; i < shownInTab.size(); i++) {
            for (int j = i + 1; j < shownInTab.size(); j++) {
                context.assertFalse(ItemStack.matches(shownInTab.get(i), shownInTab.get(j)), "the creative tab lists "
                        + shownInTab.get(i) + " " + shownInTab.get(i).getTag() + " twice, which the newer lines' tab refuses with a crash");
            }
        }

        List<ItemStack> all = new ArrayList<>(shownInTab);
        all.addAll(searched);
        // A tab that quietly built nothing would pass the throw test and fail the player, so the
        // count is asserted too rather than assumed.
        context.assertTrue(all.size() > 50,
                "the creative tab built only " + all.size() + " stacks — it did not populate");

        // Every item of ours is in the tab. "Add every new item to ModItemGroups" is a rule, and a rule
        // with no check is a suggestion: until 2026-10-08 this named seven items by hand, so an item
        // added since was checked by nothing.
        Map<String, String> missing = new TreeMap<>();
        for (Item item : Registry.ITEM) {
            ResourceLocation id = Registry.ITEM.getKey(item);
            if (Sweep.ours(id) && all.stream().noneMatch(stack -> stack.getItem() == item)) {
                missing.put(id.toString(), "is in no stack the tab or creative search shows");
            }
        }
        Sweep.report(context, "Every item of ours is in the creative tab", missing, ImmutableMap.of());

        // Everything the tab shows can be found by searching for it. PARENT_TAB_ONLY keeps a stack
        // out of the search tab, and is meant for a showcase whose full run is listed search-only;
        // the moon rocks had no such run, so search never found one.
        for (ItemStack shown : shownInTab) {
            boolean searchable = false;
            for (ItemStack stack : searched) {
                if (stack.getItem() == shown.getItem()) {
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
        for (Recipe<?> entry : world.getServer().getRecipeManager().getRecipes()) {
            ItemStack made = entry.getResultItem();
            if (!(made.getItem() == ModItems.SPLIFF || made.getItem() == ModItems.MOON_ROCK)) {
                continue;
            }
            boolean listed = false;
            for (ItemStack stack : searched) {
                if (ItemStack.isSame(stack, made) && ItemStack.tagMatches(stack, made)) {
                    listed = true;
                    break;
                }
            }
            context.assertTrue(listed, entry.getId() + " makes a stack creative search never finds");
        }
        context.succeed();
    }

    // aViewersOwnLookupStillBuildsTheTab is not on 1.16.5: a tab there is handed no lookup (it reads its
    // side's own strains, StackRegistries), so no viewer can build it with one of its own.
}
