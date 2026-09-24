package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * The 2.0.1 rebalance, through the real hit where it can be: {@code Item#use} on a packed device, so
 * the strain JSON, {@code SmokeContents}, {@code Smoking} and {@code EffectPolicy} all have their say.
 *
 * <p>A green-out is a roll this test cannot seed, so a spliff of three is smoked until it has seen
 * both a clean hit and a green-out. At one in eight, a hundred and twenty hits without a green-out is
 * about one in ten million. A spliff and not a bong, because a bong's click starts a draw here and
 * the hit lands only when the draw finishes.
 */
public final class RebalanceGameTest {
    private static final int TRIES = 120;

    public static void aBigDoseBuysTimeNotLevel(GameTestHelper context) {
        ServerPlayer clean = null;
        boolean sawGreenOut = false;
        for (int i = 0; i < TRIES && (clean == null || !sawGreenOut); i++) {
            ServerPlayer player = freshPlayer(context);
            player.getFoodData().setSaturation(5f);
            ItemStack spliff = packed(context, ModItems.SPLIFF, ModStrains.SATIVA, 3);
            smoke(context, player, spliff);
            if (player.hasEffect(MobEffects.SPEED)) {
                clean = player;
            } else {
                sawGreenOut = true;
                long lockout = spliff.getOrDefault(ModComponents.COOLDOWN_UNTIL, 0L) - context.getLevel().getGameTime();
                context.assertValueEqual((long) EffectPolicy.cooldown(GreenOut.BUILT_IN.lockoutTicks()), lockout,
                        "a full green-out locks smoking out for its minute, not the spliff's own cooldown");
                context.assertValueEqual(0f, player.getFoodData().getSaturationLevel(),
                        "a full green-out empties the saturation");
                context.assertValueEqual(1, level(player, MobEffects.SLOWNESS),
                        "a full green-out still lands its own Slowness II");
            }
        }
        context.assertTrue(clean != null && sawGreenOut, "saw both a clean hit and a green-out in " + TRIES);

        // Level II of the buffs, level III of the costs, for half as long again.
        context.assertValueEqual(1, level(clean, MobEffects.SPEED), "Speed stops at II");
        context.assertValueEqual(1, level(clean, MobEffects.HASTE), "Haste stops at II");
        context.assertValueEqual(2, level(clean, MobEffects.WEAKNESS), "Weakness still follows the dose");
        context.assertValueEqual(2, level(clean, MobEffects.HUNGER), "Hunger follows the dose to III");
        context.assertValueEqual(EffectPolicy.duration(1350), clean.getEffect(MobEffects.SPEED).getDuration(),
                "the third bud buys half the spliff's duration again");

        // Below the cap nothing is extended, and Hunger is the dose.
        ServerPlayer piper = freshPlayer(context);
        smokeUntilClean(context, piper, ModItems.WOODEN_PIPE, ModStrains.INDICA, 2);
        context.assertValueEqual(1, level(piper, MobEffects.RESISTANCE), "a pipe of two is Resistance II");
        context.assertValueEqual(1, level(piper, MobEffects.HUNGER), "a pipe of two is Hunger II");
        context.assertValueEqual(EffectPolicy.duration(700), piper.getEffect(MobEffects.RESISTANCE).getDuration(),
                "a dose at the cap keeps the pipe's own duration");
        context.succeed();
    }

    /**
     * A lower {@code maxLevel} moves the point where dose turns into time along with it. Under
     * {@code maxLevel: 1} a pipe of two is level I like a pipe of one, so its second bud has to buy
     * the extra half duration or it buys nothing but a green-out chance.
     */
    public static void aLoweredLevelCapStillBuysTime(GameTestHelper context) {
        ConfigGameTest.withConfig("{ \"effects\": { \"maxLevel\": 1 } }", () -> {
            ServerPlayer piper = freshPlayer(context);
            smokeUntilClean(context, piper, ModItems.WOODEN_PIPE, ModStrains.INDICA, 2);
            context.assertValueEqual(0, level(piper, MobEffects.RESISTANCE), "maxLevel 1 holds Resistance at I");
            context.assertValueEqual(0, level(piper, MobEffects.MINING_FATIGUE), "and the cost with it");
            context.assertValueEqual(EffectPolicy.duration(1050), piper.getEffect(MobEffects.RESISTANCE).getDuration(),
                    "a second bud past the lowered cap buys half the pipe's duration again");

            // Pairing lifts maxBuffLevel, never maxLevel, so a paired bud turns into time there too.
            List<MobEffectInstance> paired = hit(new SmokeContents(List.of(
                    new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                    new SmokeContents.Entry(strain(context, ModStrains.HASHISH), 1))));
            context.assertValueEqual(0, find(paired, MobEffects.SPEED).getAmplifier(), "maxLevel 1 holds paired Speed at I");
            context.assertValueEqual(EffectPolicy.duration(1350), find(paired, MobEffects.SPEED).getDuration(),
                    "a paired second bud past the lowered cap buys time as well");
        });
        context.succeed();
    }

    public static void aFullGreenOutEndsTheHigh(GameTestHelper context) {
        ServerPlayer full = highPlayer(context);
        Smoking.greenOut(full, 3);
        context.assertFalse(full.hasEffect(MobEffects.SPEED), "a smoked buff ends");
        context.assertFalse(full.hasEffect(MobEffects.ABSORPTION), "an edible's buff ends");
        context.assertTrue(full.hasEffect(MobEffects.HEALTH_BOOST),
                "a potion of a type the mod never grants is left alone");
        context.assertValueEqual(0f, full.getFoodData().getSaturationLevel(), "the saturation goes");
        context.assertValueEqual(600, full.getEffect(MobEffects.SLOWNESS).getDuration(), "thirty seconds of it");

        // The spins take nothing away.
        ServerPlayer spins = highPlayer(context);
        Smoking.greenOut(spins, 2);
        context.assertTrue(spins.hasEffect(MobEffects.SPEED), "the spins leave the high running");
        context.assertTrue(spins.hasEffect(MobEffects.ABSORPTION), "and the edible's");
        context.assertValueEqual(5f, spins.getFoodData().getSaturationLevel(), "and the saturation");
        context.assertValueEqual(300, spins.getEffect(MobEffects.SLOWNESS).getDuration(), "fifteen seconds of it");
        context.succeed();
    }

    public static void aBudWithAConcentrateReachesThree(GameTestHelper context) {
        // Two Lemon Haze and a pinch of hash: the bud's buffs one past the cap, its costs at the dose,
        // and the hash's own Resistance at its own count.
        List<MobEffectInstance> paired = hit(new SmokeContents(List.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                new SmokeContents.Entry(strain(context, ModStrains.HASHISH), 1))));
        context.assertValueEqual(2, find(paired, MobEffects.SPEED).getAmplifier(), "paired Speed reaches III");
        context.assertValueEqual(2, find(paired, MobEffects.HASTE).getAmplifier(), "paired Haste reaches III");
        context.assertValueEqual(1, find(paired, MobEffects.WEAKNESS).getAmplifier(), "the cost is the bud's dose, not raised");
        context.assertValueEqual(0, find(paired, MobEffects.RESISTANCE).getAmplifier(), "the hash's own buff is its own count");

        // Once: three buds and a whole rosin bowl still stop at III, and the rosin at the plain cap.
        List<MobEffectInstance> heavy = hit(new SmokeContents(List.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 3),
                new SmokeContents.Entry(strain(context, ModStrains.ROSIN), 3))));
        context.assertValueEqual(2, find(heavy, MobEffects.SPEED).getAmplifier(), "the bonus is once, not per concentrate");
        context.assertValueEqual(1, find(heavy, MobEffects.RESISTANCE).getAmplifier(), "rosin's own Resistance stays at II");

        // Scorched hemp is no concentrate: no buff of its own, so it lifts nothing.
        List<MobEffectInstance> scorched = hit(new SmokeContents(List.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                new SmokeContents.Entry(strain(context, ModStrains.SCORCHED_HEMP), 1))));
        context.assertValueEqual(1, find(scorched, MobEffects.SPEED).getAmplifier(), "scorched hemp does not pair");

        // And the hash family's Hunger scales like the plant's.
        List<MobEffectInstance> rosin = hit(SmokeContents.of(strain(context, ModStrains.ROSIN), 3));
        context.assertValueEqual(2, find(rosin, MobEffects.HUNGER).getAmplifier(), "a rosin bowl is Hunger III");
        context.succeed();
    }

    public static void ediblesBuyTimeNotLevel(GameTestHelper context) {
        List<MobEffectInstance> perfect = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 4, Quality.PERFECT, 0));
        int full = EdibleEffects.durationTicks(EdibleBundle.BUILT_IN, Quality.PERFECT);
        context.assertValueEqual(1, find(perfect, MobEffects.ABSORPTION).getAmplifier(), "Absorption stops at II");
        context.assertValueEqual(0, find(perfect, MobEffects.RESISTANCE).getAmplifier(), "Resistance I at tier IV");
        context.assertValueEqual(0, find(perfect, MobEffects.REGENERATION).getAmplifier(), "Regeneration I");
        context.assertValueEqual(0, find(perfect, MobEffects.SLOWNESS).getAmplifier(), "Perfect is Slowness I");
        context.assertValueEqual(full, find(perfect, MobEffects.SLOWNESS).getDuration(), "tier IV runs the full time");
        context.assertTrue(find(perfect, MobEffects.HUNGER).getDuration() > full / 2,
                "the munchies last the high, not a minute");

        List<MobEffectInstance> rough = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 4, Quality.ROUGH, 0));
        context.assertValueEqual(1, find(rough, MobEffects.SLOWNESS).getAmplifier(), "a Rough tier IV is Slowness II");

        List<MobEffectInstance> weak = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 1, Quality.PERFECT, 0));
        context.assertValueEqual(full * 5 / 8, find(weak, MobEffects.SLOWNESS).getDuration(),
                "tier I lasts five eighths of tier IV");
        context.assertValueEqual(0, find(weak, MobEffects.ABSORPTION).getAmplifier(), "tier I is Absorption I");
        context.succeed();
    }

    /**
     * Every quality's onset can land anywhere in its window, nowhere else, and with no pile-up at an
     * edge -- the old centre-and-clamp roll put a tenth of Rough's onsets on exactly its first tick
     * and none in its last fifteen seconds. And the windows keep their order: a worse batch waits
     * longer and less predictably, never the other way round.
     */
    public static void everyEdibleOnsetFillsItsWindow(GameTestHelper context) {
        java.util.Random random = new java.util.Random(48L);
        int rolls = 20000;
        EdibleBundle.Onset previous = null;
        for (Quality quality : List.of(Quality.ROUGH, Quality.STANDARD, Quality.CLEAN, Quality.PERFECT)) {
            EdibleBundle.Onset window = EdibleBundle.BUILT_IN.qualities().of(quality).onset();
            int width = window.maxTicks() - window.minTicks();
            int atMin = 0, atMax = 0, early = 0, late = 0;
            long sum = 0;
            for (int i = 0; i < rolls; i++) {
                int onset = EdibleEffects.rollOnsetTicks(EdibleBundle.BUILT_IN, quality, random);
                context.assertTrue(onset >= window.minTicks() && onset <= window.maxTicks(),
                        quality + " rolled an onset of " + onset + " outside " + window.minTicks() + ".." + window.maxTicks());
                atMin += onset == window.minTicks() ? 1 : 0;
                atMax += onset == window.maxTicks() ? 1 : 0;
                early += onset < window.minTicks() + width / 20 ? 1 : 0;
                late += onset > window.maxTicks() - width / 20 ? 1 : 0;
                sum += onset;
            }
            context.assertTrue(atMin < rolls / 100 && atMax < rolls / 100,
                    quality + " piles onsets on an edge of its window: " + atMin + " on the first tick, " + atMax + " on the last");
            context.assertTrue(early > 0 && late > 0,
                    quality + " never reaches an end of its window: " + early + " in the first twentieth, " + late + " in the last");
            double mean = (window.minTicks() + window.modeTicks() + window.maxTicks()) / 3.0;
            context.assertTrue(Math.abs(sum / (double) rolls - mean) < Math.max(2, width / 50.0),
                    quality + "'s onsets average " + sum / rolls + " ticks, not the window's " + (int) mean);
            if (previous != null) {
                context.assertTrue(window.maxTicks() - window.minTicks() < previous.maxTicks() - previous.minTicks()
                                && window.modeTicks() <= previous.modeTicks(),
                        quality + " is no surer or sooner than the quality below it");
            }
            previous = window;
        }
        context.succeed();
    }

    /**
     * An edible still on its way down is part of the player's save: written out and read back into
     * another player it is the same doses, to the tick, so a logout or a server restart loses nothing.
     * Until 2.1 it was a static queue that neither knew about.
     */
    public static void aPendingEdibleIsSavedWithThePlayer(GameTestHelper context) {
        ServerPlayer eater = freshPlayer(context);
        EdibleEffects.consume(eater, 4, Quality.PERFECT);
        List<EdibleScheduler.Pending> waiting = EdibleScheduler.pending(eater);
        context.assertValueEqual(EdibleBundle.BUILT_IN.effects().size(), waiting.size(), "doses waiting after eating a tier-IV edible");
        context.assertTrue(eater.getActiveEffects().isEmpty(), "an edible landed the moment it was eaten");

        var registries = context.getLevel().registryAccess();
        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        eater.saveWithoutId(saved);
        ServerPlayer back = freshPlayer(context);
        context.assertTrue(EdibleScheduler.pending(back).isEmpty(), "a fresh player already has doses waiting");
        back.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved.buildResult()));
        context.assertValueEqual(waiting, EdibleScheduler.pending(back), "the doses read back from the save");
        context.succeed();
    }

    /** A dose lands on the tick its wait runs out, and leaves nothing behind on the player. */
    public static void aPendingDoseLandsOnTime(GameTestHelper context) {
        ServerPlayer eater = freshPlayer(context);
        EdibleScheduler.schedule(eater, 5, new MobEffectInstance(MobEffects.SLOWNESS, 200, 0));
        context.runAtTickTime(4, () -> context.assertTrue(!eater.hasEffect(MobEffects.SLOWNESS),
                "a dose due in five ticks landed early"));
        context.runAtTickTime(8, () -> {
            context.assertTrue(eater.hasEffect(MobEffects.SLOWNESS), "a dose due in five ticks never landed");
            context.assertTrue(EdibleScheduler.pending(eater).isEmpty(), "a landed dose is still waiting on the player");
            context.succeed();
        });
    }

    // ---------------------------------------------------------------------

    private static ServerPlayer freshPlayer(GameTestHelper context) {
        return context.makeMockServerPlayerInLevel();
    }

    private static ServerPlayer highPlayer(GameTestHelper context) {
        ServerPlayer player = freshPlayer(context);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 1000, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1000, 1));
        player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 1000, 0));
        player.getFoodData().setSaturation(5f);
        return player;
    }

    /** What one hit of {@code contents} applies after the config, the way Smoking hands it over. */
    private static List<MobEffectInstance> hit(SmokeContents contents) {
        return EffectPolicy.filter(contents.effects(900, false, ThreadLocalRandom.current()), contents.buffBonus());
    }

    private static Holder<Strain> strain(GameTestHelper context, ResourceKey<Strain> key) {
        return context.getLevel().registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static ItemStack packed(GameTestHelper context, Item device, ResourceKey<Strain> strain, int dose) {
        Holder<Strain> entry = strain(context, strain);
        ItemStack stack = new ItemStack(device);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(entry, dose));
        stack.set(ModComponents.CHARGES, 4);
        return stack;
    }

    private static void smoke(GameTestHelper context, ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(context.getLevel(), player, InteractionHand.MAIN_HAND);
    }

    private static void smokeUntilClean(GameTestHelper context, ServerPlayer player, Item device,
                                        ResourceKey<Strain> strain, int dose) {
        for (int i = 0; i < TRIES; i++) {
            player.removeAllEffects();
            player.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(device));
            smoke(context, player, packed(context, device, strain, dose));
            if (player.hasEffect(MobEffects.RESISTANCE)) {
                return;
            }
        }
        context.fail("never a clean hit in " + TRIES);
    }

    private static int level(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }

    private static List<MobEffectInstance> allowed(List<EdibleEffects.Dose> bundle) {
        return EffectPolicy.filterKeepingDuration(bundle.stream().map(EdibleEffects.Dose::effect).toList());
    }

    private static MobEffectInstance find(List<MobEffectInstance> effects, Holder<MobEffect> type) {
        return effects.stream().filter(e -> e.getEffect().equals(type)).findFirst()
                .orElseThrow(() -> new AssertionError("no " + type.getRegisteredName() + " in the bundle"));
    }
}
