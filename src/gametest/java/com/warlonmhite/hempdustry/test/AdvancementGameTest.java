package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.util.Optional;

/**
 * The advancements keyed on eating and drinking, driven against the tree <b>as the server loaded
 * it</b>. A test that rebuilt the criteria would agree with a wrong one; the risk lives in the
 * generated JSON and in the tags it names.
 *
 * <p>Each food is consumed through {@code Item#finishUsing}, the same call the end of an eating
 * animation makes, so {@code minecraft:consume_item} fires from vanilla's own site. Every positive
 * has a negative beside it: a criterion that had widened to "any food" would pass the positives
 * alone.
 */
public final class AdvancementGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void eatingGrantsTheFoodNodes(TestContext context) {
        AdvancementEntry hempHearts = loaded(context, "hemp_hearts", "hempdustry");
        AdvancementEntry gotBhang = loaded(context, "got_bhang", "activation_energy");
        AdvancementEntry club = loaded(context, "club_des_hashischins", "butter_late_than_never");

        // Bread is a food and in no tag of ours; cannabutter toast is an edible but neither the jam
        // nor the drink. Neither may grant anything here.
        ServerPlayerEntity player = freshPlayer(context);
        consume(context, player, Items.BREAD);
        consume(context, player, ModItems.CANNABUTTER_TOAST);
        assertNotDone(context, player, hempHearts, "bread or cannabutter toast");
        assertNotDone(context, player, gotBhang, "bread or cannabutter toast");
        assertNotDone(context, player, club, "bread or cannabutter toast");

        // Every seed food grants Hemp Hearts on its own — a fresh player each, so one entry that
        // had fallen out of the tag cannot hide behind another.
        for (Item food : new Item[]{ModItems.TOASTED_HEMP_SEEDS, ModItems.HEMP_FLAPJACK, ModItems.SIEMIENIOTKA}) {
            ServerPlayerEntity eater = freshPlayer(context);
            consume(context, eater, food);
            context.assertTrue(eater.getAdvancementTracker().getProgress(hempHearts).isDone(),
                    "eating " + food + " did not grant Hemp Hearts — it has left #hempdustry:hemp_seed_foods");
            assertNotDone(context, eater, club, food.toString());
        }

        // Hemp milk is seeds too, but it is drunk and it is an Infuser ingredient: not a food.
        ServerPlayerEntity drinker = freshPlayer(context);
        consume(context, drinker, ModItems.HEMP_MILK_BUCKET);
        assertNotDone(context, drinker, hempHearts, "hemp milk");

        ServerPlayerEntity bhang = freshPlayer(context);
        consume(context, bhang, ModItems.BHANG_BUCKET);
        context.assertTrue(bhang.getAdvancementTracker().getProgress(gotBhang).isDone(),
                "drinking bhang did not grant Got Bhang?");
        assertNotDone(context, bhang, club, "bhang");

        ServerPlayerEntity jam = freshPlayer(context);
        consume(context, jam, ModItems.DAWAMESK);
        context.assertTrue(jam.getAdvancementTracker().getProgress(club).isDone(),
                "eating dawamesk did not grant Club des Hashischins");
        assertNotDone(context, jam, gotBhang, "dawamesk");

        context.complete();
    }

    /** Fetches a loaded advancement and checks where it hangs, which is half of what it teaches. */
    private static AdvancementEntry loaded(TestContext context, String id, String parent) {
        AdvancementEntry entry = context.getWorld().getServer().getAdvancementLoader()
                .get(Identifier.of("hempdustry", id));
        context.assertTrue(entry != null, "hempdustry:" + id + " is not loaded at all");
        context.assertTrue(entry.value().parent().equals(Optional.of(Identifier.of("hempdustry", parent))),
                "hempdustry:" + id + " hangs under " + entry.value().parent() + ", not hempdustry:" + parent);
        return entry;
    }

    /** Survival, so the stack is really spent and the player is not treated as a creative tester. */
    private static ServerPlayerEntity freshPlayer(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        return player;
    }

    private static void consume(TestContext context, ServerPlayerEntity player, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getItem().finishUsing(stack, context.getWorld(), player);
    }

    private static void assertNotDone(TestContext context, ServerPlayerEntity player, AdvancementEntry entry,
                                      String what) {
        context.assertFalse(player.getAdvancementTracker().getProgress(entry).isDone(),
                what + " granted " + entry.id() + ", so its criterion matches more than it should");
    }
}
