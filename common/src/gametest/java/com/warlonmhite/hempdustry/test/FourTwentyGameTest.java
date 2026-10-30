package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.advancement.SmokeCriterion;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 4:20 as a named moment of the overworld's day, and the munchies as an advancement. Both rest on
 * 26.1's data: a timeline's {@code time_markers} (which {@code /time set} offers when
 * {@code show_in_commands} is on) and the player predicate's {@code food}.
 */
public final class FourTwentyGameTest {
    private static final ResourceKey<ClockTimeMarker> FOUR_TWENTY = ResourceKey.create(ClockTimeMarkers.ROOT_ID,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "four_twenty"));

    /**
     * The marker is on the overworld clock, offered by {@code /time set}, and sits at 4:20 PM: tick 0
     * is 6 AM and an hour is 1000 ticks, so 10 h 20 min after it is tick 10333, the centre of
     * <i>Blaze It!</i>'s window.
     *
     * <p>Read off the registry rather than by moving the clock there, which would change the time for
     * every other test running beside this one.
     */
    public static void fourTwentyIsATimeMarker(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        Holder<WorldClock> overworld = server.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        context.assertTrue(server.clockManager().commandTimeMarkersForClock(overworld).anyMatch(FOUR_TWENTY::equals),
                "/time set does not offer hempdustry:four_twenty on the overworld clock");

        Map<ResourceKey<ClockTimeMarker>, ClockTimeMarker> markers = new HashMap<>();
        server.registryAccess().lookupOrThrow(Registries.TIMELINE).listElements()
                .forEach(timeline -> timeline.value().registerTimeMarkers(markers::put));
        ClockTimeMarker marker = markers.get(FOUR_TWENTY);
        context.assertTrue(marker != null && marker.clock().is(WorldClocks.OVERWORLD),
                "hempdustry:four_twenty is not a marker of the overworld clock: " + marker);
        context.assertTrue(marker.ticks() == 10333 && marker.periodTicks().equals(Optional.of(24000)),
                "hempdustry:four_twenty is at tick " + marker.ticks() + " of " + marker.periodTicks()
                        + ", not 10333 of every 24000-tick day");

        AdvancementHolder blazeIt = server.getAdvancements().get(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "blaze_it"));
        SmokeCriterion.TimeWindow window = ((SmokeCriterion.Conditions) blazeIt.value().criteria()
                .get("smoked_at_420").triggerInstance()).time().orElseThrow();
        context.assertTrue(marker.ticks() == (window.min() + window.max()) / 2,
                "the 4:20 marker (" + marker.ticks() + ") is not the centre of Blaze It!'s window "
                        + window.min() + "..." + window.max());
        context.succeed();
    }

    /** Eating while under Hunger with the food bar at 6 or under, and nothing short of all three. */
    public static void theMunchiesWantHungerAndAnEmptyStomach(GameTestHelper context) {
        AdvancementHolder munchies = context.getLevel().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "the_munchies"));
        context.assertTrue(munchies != null, "hempdustry:the_munchies is not loaded");
        context.assertTrue(munchies.value().parent().equals(Optional.of(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "first_contact"))),
                "The Munchies hangs under " + munchies.value().parent() + ", not First Contact");

        context.assertTrue(eats(context, true, 6, Items.BREAD, munchies), "bread at 6 under Hunger did not grant The Munchies");
        context.assertTrue(eats(context, true, 0, Items.COOKIE, munchies), "a cookie at 0 under Hunger did not grant The Munchies");
        context.assertFalse(eats(context, false, 6, Items.BREAD, munchies), "bread at 6 with no Hunger granted The Munchies");
        context.assertFalse(eats(context, true, 7, Items.BREAD, munchies),
                "bread at 7 under Hunger granted The Munchies: the bar was read after the food landed, or the bound moved");
        context.assertFalse(eats(context, true, 6, Items.MILK_BUCKET, munchies), "a drink with no food in it granted The Munchies");
        context.succeed();
    }

    private static boolean eats(GameTestHelper context, boolean hunger, int foodLevel, Item food, AdvancementHolder entry) {
        ServerPlayer player = Players.survival(context);
        if (hunger) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600));
        }
        player.getFoodData().setFoodLevel(foodLevel);
        ItemStack stack = new ItemStack(food);
        stack.getItem().finishUsingItem(stack, context.getLevel(), player);
        return player.getAdvancements().getOrStartProgress(entry).isDone();
    }
}
