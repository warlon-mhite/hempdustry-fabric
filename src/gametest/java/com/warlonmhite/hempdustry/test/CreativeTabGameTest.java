package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The creative tab is built without throwing — <b>and this test exists because it once did.</b>
 *
 * <h2>The bug, and why nothing caught it</h2>
 *
 * On 2026-09-09 an edit to {@code ModItemGroups} added
 * {@code entries.add(WASHED_DECARBOXYLATED_HEMP)} without removing the existing one. Vanilla's
 * {@code ItemGroup$EntriesImpl.add} refuses that outright —
 * <i>"Accidentally adding the same item stack twice"</i>, an {@code IllegalStateException} — so
 * <b>opening the creative inventory crashed the client, in every world.</b>
 *
 * <p>It reached a play-test through a full green board: {@code build}, {@code runDatagen},
 * {@code recipe_collisions.py} and sixteen game tests all passed. They passed because
 * <b>{@code runGametest} boots a dedicated server, and a dedicated server never populates a creative
 * tab</b> — {@code ItemGroups.updateEntries} runs from {@code CreativeInventoryScreen}, on the
 * client, when a player presses E. The mod's largest and most-edited lambda had no coverage at all.
 *
 * <p><b>It did not have to be that way, and that is the actual lesson.</b>
 * {@code net.minecraft.item.ItemGroups} and {@code ItemGroup#updateEntries} are <em>common</em> code,
 * not client code: a headless server can build the tab perfectly well: it simply had never been
 * asked to. "It only happens on a client" was an assumption, not a fact — and the way to check an
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

    /**
     * Items whose presence is asserted by name. Not the whole registry — a few registered items are
     * deliberately absent from the tab (potted flowers, wall signs) — but every material the
     * extraction bench added, because "add every new item to {@code ModItemGroups}" is a rule, and a
     * rule with no check is a suggestion.
     */
    private static final List<Item> MUST_APPEAR = List.of(
            ModItems.KIEF, ModItems.BUBBLE_HASH, ModItems.HASHISH, ModItems.FILTERED_HASHISH,
            ModItems.CHARAS, ModItems.ROSIN, ModItems.MOON_ROCK);

    public static void creativeTabPopulates(TestContext context) {
        ServerWorld world = context.getWorld();

        // Exactly what CreativeInventoryScreen does on opening, minus the screen. A duplicate stack
        // throws IllegalStateException from inside here, which fails the test as an error rather
        // than as an assertion -- which is the right shape: this is "does it work at all".
        ItemGroup.DisplayContext display = new ItemGroup.DisplayContext(
                world.getEnabledFeatures(), true, world.getRegistryManager());
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.updateEntries(display);

        List<ItemStack> all = new ArrayList<>(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayStacks());
        all.addAll(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabStacks());
        // A tab that quietly built nothing would pass the throw test and fail the player, so the
        // count is asserted too rather than assumed.
        context.assertTrue(all.size() > 50,
                "the creative tab built only " + all.size() + " stacks — it did not populate");

        for (Item item : MUST_APPEAR) {
            boolean found = false;
            for (ItemStack stack : all) {
                if (stack.isOf(item)) {
                    found = true;
                    break;
                }
            }
            context.assertTrue(found, "the creative tab is missing " + item);
        }
        context.complete();
    }
}
