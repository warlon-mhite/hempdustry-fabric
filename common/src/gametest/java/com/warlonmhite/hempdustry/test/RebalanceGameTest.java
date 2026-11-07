package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.registry.DataRegistries;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.Registry;
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
import com.warlonmhite.hempdustry.registry.Holder;
import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aBigDoseBuysTimeNotLevel(GameTestHelper context) {
        ServerPlayer clean = null;
        boolean sawGreenOut = false;
        for (int i = 0; i < TRIES && (clean == null || !sawGreenOut); i++) {
            ServerPlayer player = freshPlayer(context);
            Line.setSaturation(player, 5f);
            ItemStack spliff = packed(context, ModItems.SPLIFF, ModStrains.SATIVA, 3);
            smoke(context, player, spliff);
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                clean = player;
            } else {
                sawGreenOut = true;
                long lockout = ModComponents.COOLDOWN_UNTIL.getOrDefault(spliff, 0L) - context.getLevel().getGameTime();
                Line.equal(context, (long) EffectPolicy.cooldown(GreenOut.BUILT_IN.lockoutTicks()), lockout,
                        "a full green-out locks smoking out for its minute, not the spliff's own cooldown");
                Line.equal(context, 0f, player.getFoodData().getSaturationLevel(),
                        "a full green-out empties the saturation");
                Line.equal(context, 1, level(player, MobEffects.MOVEMENT_SLOWDOWN),
                        "a full green-out still lands its own Slowness II");
            }
        }
        context.assertTrue(clean != null && sawGreenOut, "saw both a clean hit and a green-out in " + TRIES);

        // Level II of the buffs, level III of the costs, for half as long again.
        Line.equal(context, 1, level(clean, MobEffects.MOVEMENT_SPEED), "Speed stops at II");
        Line.equal(context, 1, level(clean, MobEffects.DIG_SPEED), "Haste stops at II");
        Line.equal(context, 2, level(clean, MobEffects.WEAKNESS), "Weakness still follows the dose");
        Line.equal(context, 2, level(clean, MobEffects.HUNGER), "Hunger follows the dose to III");
        Line.equal(context, clean.getEffect(MobEffects.MOVEMENT_SPEED).getDuration(), EffectPolicy.duration(1350),
                "the third bud buys half the spliff's duration again");

        // Below the cap nothing is extended, and Hunger is the dose.
        ServerPlayer piper = freshPlayer(context);
        smokeUntilClean(context, piper, ModItems.WOODEN_PIPE, ModStrains.INDICA, 2);
        Line.equal(context, 1, level(piper, MobEffects.DAMAGE_RESISTANCE), "a pipe of two is Resistance II");
        Line.equal(context, 1, level(piper, MobEffects.HUNGER), "a pipe of two is Hunger II");
        Line.equal(context, EffectPolicy.duration(700), piper.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration(),
                "a dose at the cap keeps the pipe's own duration");
        context.succeed();
    }

    /**
     * A lower {@code maxLevel} moves the point where dose turns into time along with it. Under
     * {@code maxLevel: 1} a pipe of two is level I like a pipe of one, so its second bud has to buy
     * the extra half duration or it buys nothing but a green-out chance.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aLoweredLevelCapStillBuysTime(GameTestHelper context) {
        ConfigGameTest.withConfig("{ \"effects\": { \"maxLevel\": 1 } }", () -> {
            ServerPlayer piper = freshPlayer(context);
            smokeUntilClean(context, piper, ModItems.WOODEN_PIPE, ModStrains.INDICA, 2);
            Line.equal(context, 0, level(piper, MobEffects.DAMAGE_RESISTANCE), "maxLevel 1 holds Resistance at I");
            Line.equal(context, 0, level(piper, MobEffects.DIG_SLOWDOWN), "and the cost with it");
            Line.equal(context, EffectPolicy.duration(1050), piper.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration(),
                    "a second bud past the lowered cap buys half the pipe's duration again");

            // Pairing lifts maxBuffLevel, never maxLevel, so a paired bud turns into time there too.
            List<MobEffectInstance> paired = hit(new SmokeContents(ImmutableList.of(
                    new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                    new SmokeContents.Entry(strain(context, ModStrains.HASHISH), 1))));
            Line.equal(context, find(paired, MobEffects.MOVEMENT_SPEED).getAmplifier(), 0, "maxLevel 1 holds paired Speed at I");
            Line.equal(context, find(paired, MobEffects.MOVEMENT_SPEED).getDuration(), EffectPolicy.duration(1350),
                    "a paired second bud past the lowered cap buys time as well");
        });
        context.succeed();
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aFullGreenOutEndsTheHigh(GameTestHelper context) {
        ServerPlayer full = highPlayer(context);
        Smoking.greenOut(full, 3);
        context.assertFalse(full.hasEffect(MobEffects.MOVEMENT_SPEED), "a smoked buff ends");
        context.assertFalse(full.hasEffect(MobEffects.ABSORPTION), "an edible's buff ends");
        context.assertTrue(full.hasEffect(MobEffects.FIRE_RESISTANCE),
                "a potion of a type the mod never grants is left alone");
        Line.equal(context, 0f, full.getFoodData().getSaturationLevel(), "the saturation goes");
        Line.equal(context, 600, full.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration(), "thirty seconds of it");

        // The spins take nothing away.
        ServerPlayer spins = highPlayer(context);
        Smoking.greenOut(spins, 2);
        context.assertTrue(spins.hasEffect(MobEffects.MOVEMENT_SPEED), "the spins leave the high running");
        context.assertTrue(spins.hasEffect(MobEffects.ABSORPTION), "and the edible's");
        Line.equal(context, 5f, spins.getFoodData().getSaturationLevel(), "and the saturation");
        Line.equal(context, 300, spins.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration(), "fifteen seconds of it");
        context.succeed();
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aBudWithAConcentrateReachesThree(GameTestHelper context) {
        // Two Lemon Haze and a pinch of hash: the bud's buffs one past the cap, its costs at the dose,
        // and the hash's own Resistance at its own count.
        List<MobEffectInstance> paired = hit(new SmokeContents(ImmutableList.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                new SmokeContents.Entry(strain(context, ModStrains.HASHISH), 1))));
        Line.equal(context, find(paired, MobEffects.MOVEMENT_SPEED).getAmplifier(), 2, "paired Speed reaches III");
        Line.equal(context, find(paired, MobEffects.DIG_SPEED).getAmplifier(), 2, "paired Haste reaches III");
        Line.equal(context, find(paired, MobEffects.WEAKNESS).getAmplifier(), 1, "the cost is the bud's dose, not raised");
        Line.equal(context, find(paired, MobEffects.DAMAGE_RESISTANCE).getAmplifier(), 0, "the hash's own buff is its own count");

        // Once: three buds and a whole rosin bowl still stop at III, and the rosin at the plain cap.
        List<MobEffectInstance> heavy = hit(new SmokeContents(ImmutableList.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 3),
                new SmokeContents.Entry(strain(context, ModStrains.ROSIN), 3))));
        Line.equal(context, find(heavy, MobEffects.MOVEMENT_SPEED).getAmplifier(), 2, "the bonus is once, not per concentrate");
        Line.equal(context, find(heavy, MobEffects.DAMAGE_RESISTANCE).getAmplifier(), 1, "rosin's own Resistance stays at II");

        // Scorched hemp is no concentrate: no buff of its own, so it lifts nothing.
        List<MobEffectInstance> scorched = hit(new SmokeContents(ImmutableList.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                new SmokeContents.Entry(strain(context, ModStrains.SCORCHED_HEMP), 1))));
        Line.equal(context, find(scorched, MobEffects.MOVEMENT_SPEED).getAmplifier(), 1, "scorched hemp does not pair");

        // And the hash family's Hunger scales like the plant's.
        List<MobEffectInstance> rosin = hit(SmokeContents.of(strain(context, ModStrains.ROSIN), 3));
        Line.equal(context, find(rosin, MobEffects.HUNGER).getAmplifier(), 2, "a rosin bowl is Hunger III");
        context.succeed();
    }

    /**
     * A loaded device does not break while hits are left in its bowl: it holds on at its last point
     * of durability and breaks on the hit that empties the bowl.
     *
     * <p>Every device's durability is a whole number of bowls new, but repairs, Unbreaking, Mending
     * and anvil-combining all put it out of step, and the device used to break on whichever hit
     * took the last point, taking the rest of the bowl with it.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aLoadedDeviceDoesNotBreakMidBowl(GameTestHelper context) {
        ServerPlayer player = Players.mock(context);
        player.setGameMode(GameType.SURVIVAL);
        ItemStack pipe = packed(context, ModItems.WOODEN_PIPE, ModStrains.INDICA, 1);
        int charges = ModComponents.CHARGES.getOrDefault(pipe, 0);
        pipe.setDamageValue(pipe.getMaxDamage() - 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        for (int hit = 1; hit <= charges; hit++) {
            ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            context.assertFalse(inHand.isEmpty(), "the pipe broke with " + (charges - hit + 1) + " hits left in its bowl");
            Line.clearCooldown(player, ModItems.WOODEN_PIPE);
            smoke(context, player, inHand);
        }
        context.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "the pipe outlived the hit that emptied its bowl at no durability");
        context.succeed();
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void ediblesBuyTimeNotLevel(GameTestHelper context) {
        List<MobEffectInstance> perfect = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 4, Quality.PERFECT, 0));
        int full = EdibleEffects.durationTicks(EdibleBundle.BUILT_IN, Quality.PERFECT);
        Line.equal(context, 1, find(perfect, MobEffects.ABSORPTION).getAmplifier(), "Absorption stops at II");
        Line.equal(context, 0, find(perfect, MobEffects.DAMAGE_RESISTANCE).getAmplifier(), "Resistance I at tier IV");
        Line.equal(context, 0, find(perfect, MobEffects.REGENERATION).getAmplifier(), "Regeneration I");
        Line.equal(context, 0, find(perfect, MobEffects.MOVEMENT_SLOWDOWN).getAmplifier(), "Perfect is Slowness I");
        Line.equal(context, full, find(perfect, MobEffects.MOVEMENT_SLOWDOWN).getDuration(), "tier IV runs the full time");
        context.assertTrue(find(perfect, MobEffects.HUNGER).getDuration() > full / 2,
                "the munchies last the high, not a minute");

        List<MobEffectInstance> rough = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 4, Quality.ROUGH, 0));
        Line.equal(context, 1, find(rough, MobEffects.MOVEMENT_SLOWDOWN).getAmplifier(), "a Rough tier IV is Slowness II");

        List<MobEffectInstance> weak = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 1, Quality.PERFECT, 0));
        Line.equal(context, full * 5 / 8, find(weak, MobEffects.MOVEMENT_SLOWDOWN).getDuration(),
                "tier I lasts five eighths of tier IV");
        Line.equal(context, 0, find(weak, MobEffects.ABSORPTION).getAmplifier(), "tier I is Absorption I");
        context.succeed();
    }

    /**
     * Every quality's onset can land anywhere in its window, nowhere else, and with no pile-up at an
     * edge -- the old centre-and-clamp roll put a tenth of Rough's onsets on exactly its first tick
     * and none in its last fifteen seconds. And the windows keep their order: a worse batch waits
     * longer and less predictably, never the other way round.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyEdibleOnsetFillsItsWindow(GameTestHelper context) {
        java.util.Random random = new java.util.Random(48L);
        int rolls = 20000;
        EdibleBundle.Onset previous = null;
        for (Quality quality : ImmutableList.of(Quality.ROUGH, Quality.STANDARD, Quality.CLEAN, Quality.PERFECT)) {
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
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aPendingEdibleIsSavedWithThePlayer(GameTestHelper context) {
        ServerPlayer eater = freshPlayer(context);
        EdibleEffects.consume(eater, 4, Quality.PERFECT);
        List<EdibleScheduler.Pending> waiting = EdibleScheduler.pending(eater);
        Line.equal(context, waiting.size(), EdibleBundle.BUILT_IN.effects().size(), "doses waiting after eating a tier-IV edible");
        context.assertTrue(eater.getActiveEffects().isEmpty(), "an edible landed the moment it was eaten");

        CompoundTag saved = eater.saveWithoutId(new CompoundTag());
        ServerPlayer back = freshPlayer(context);
        context.assertTrue(EdibleScheduler.pending(back).isEmpty(), "a fresh player already has doses waiting");
        back.load(saved);
        Line.equal(context, EdibleScheduler.pending(back), waiting, "the doses read back from the save");
        context.succeed();
    }

    /** A dose lands on the tick its wait runs out, and leaves nothing behind on the player. */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aPendingDoseLandsOnTime(GameTestHelper context) {
        ServerPlayer eater = freshPlayer(context);
        EdibleScheduler.schedule(eater, 5, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
        context.runAtTickTime(4, () -> context.assertTrue(!eater.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "a dose due in five ticks landed early"));
        context.runAtTickTime(8, () -> {
            context.assertTrue(eater.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a dose due in five ticks never landed");
            context.assertTrue(EdibleScheduler.pending(eater).isEmpty(), "a landed dose is still waiting on the player");
            context.succeed();
        });
    }

    // ---------------------------------------------------------------------

    private static ServerPlayer freshPlayer(GameTestHelper context) {
        return Players.mock(context);
    }

    private static ServerPlayer highPlayer(GameTestHelper context) {
        ServerPlayer player = freshPlayer(context);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1000, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1000, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000, 0));
        Line.setSaturation(player, 5f);
        return player;
    }

    /** What one hit of {@code contents} applies after the config, the way Smoking hands it over. */
    private static List<MobEffectInstance> hit(SmokeContents contents) {
        return EffectPolicy.filter(contents.effects(900, false, ThreadLocalRandom.current()), contents.buffBonus());
    }

    private static Holder<Strain> strain(GameTestHelper context, ResourceKey<Strain> key) {
        return DataRegistries.of(context.getLevel()).lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static ItemStack packed(GameTestHelper context, Item device, ResourceKey<Strain> strain, int dose) {
        Holder<Strain> entry = strain(context, strain);
        ItemStack stack = new ItemStack(device);
        ModComponents.SMOKE_CONTENTS.set(stack, SmokeContents.of(entry, dose));
        ModComponents.CHARGES.set(stack, 4);
        return stack;
    }

    private static void smoke(GameTestHelper context, ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(context.getLevel(), player, InteractionHand.MAIN_HAND);
        // A bong's hit lands at the end of its draw, not on the click: finish the draw.
        if (stack.getUseDuration() > 0) {
            stack.getItem().finishUsingItem(stack, context.getLevel(), player);
        }
    }

    private static void smokeUntilClean(GameTestHelper context, ServerPlayer player, Item device,
                                        ResourceKey<Strain> strain, int dose) {
        for (int i = 0; i < TRIES; i++) {
            player.removeAllEffects();
            Line.clearCooldown(player, device);
            smoke(context, player, packed(context, device, strain, dose));
            if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                return;
            }
        }
        context.fail("never a clean hit in " + TRIES);
    }

    private static int level(ServerPlayer player, MobEffect effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }

    private static List<MobEffectInstance> allowed(List<EdibleEffects.Dose> bundle) {
        return EffectPolicy.filterKeepingDuration(bundle.stream().map(EdibleEffects.Dose::effect).collect(java.util.stream.Collectors.toList()));
    }

    private static MobEffectInstance find(List<MobEffectInstance> effects, MobEffect type) {
        return effects.stream().filter(e -> e.getEffect().equals(type)).findFirst()
                .orElseThrow(() -> new AssertionError("no " + Registry.MOB_EFFECT.getKey(type) + " in the bundle"));
    }
}
