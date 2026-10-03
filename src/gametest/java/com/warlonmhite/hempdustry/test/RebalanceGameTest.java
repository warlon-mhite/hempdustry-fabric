package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;

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

    public static void aBigDoseBuysTimeNotLevel(TestContext context) {
        ServerPlayerEntity clean = null;
        boolean sawGreenOut = false;
        for (int i = 0; i < TRIES && (clean == null || !sawGreenOut); i++) {
            ServerPlayerEntity player = freshPlayer(context);
            player.getHungerManager().setSaturationLevel(5f);
            ItemStack spliff = packed(context, ModItems.SPLIFF, ModStrains.SATIVA, 3);
            smoke(context, player, spliff);
            if (player.hasStatusEffect(StatusEffects.SPEED)) {
                clean = player;
            } else {
                sawGreenOut = true;
                long lockout = spliff.getOrDefault(ModComponents.COOLDOWN_UNTIL, 0L) - context.getWorld().getTime();
                context.assertEquals((long) EffectPolicy.cooldown(GreenOut.BUILT_IN.lockoutTicks()), lockout,
                        "a full green-out locks smoking out for its minute, not the spliff's own cooldown");
                context.assertEquals(0f, player.getHungerManager().getSaturationLevel(),
                        "a full green-out empties the saturation");
                context.assertEquals(1, level(player, StatusEffects.SLOWNESS),
                        "a full green-out still lands its own Slowness II");
            }
        }
        context.assertTrue(clean != null && sawGreenOut, "saw both a clean hit and a green-out in " + TRIES);

        // Level II of the buffs, level III of the costs, for half as long again.
        context.assertEquals(1, level(clean, StatusEffects.SPEED), "Speed stops at II");
        context.assertEquals(1, level(clean, StatusEffects.HASTE), "Haste stops at II");
        context.assertEquals(2, level(clean, StatusEffects.WEAKNESS), "Weakness still follows the dose");
        context.assertEquals(2, level(clean, StatusEffects.HUNGER), "Hunger follows the dose to III");
        context.assertEquals(EffectPolicy.duration(1350), clean.getStatusEffect(StatusEffects.SPEED).getDuration(),
                "the third bud buys half the spliff's duration again");

        // Below the cap nothing is extended, and Hunger is the dose.
        ServerPlayerEntity piper = freshPlayer(context);
        smokeUntilClean(context, piper, ModItems.WOODEN_PIPE, ModStrains.INDICA, 2);
        context.assertEquals(1, level(piper, StatusEffects.RESISTANCE), "a pipe of two is Resistance II");
        context.assertEquals(1, level(piper, StatusEffects.HUNGER), "a pipe of two is Hunger II");
        context.assertEquals(EffectPolicy.duration(700), piper.getStatusEffect(StatusEffects.RESISTANCE).getDuration(),
                "a dose at the cap keeps the pipe's own duration");
        context.complete();
    }

    /**
     * A lower {@code maxLevel} moves the point where dose turns into time along with it. Under
     * {@code maxLevel: 1} a pipe of two is level I like a pipe of one, so its second bud has to buy
     * the extra half duration or it buys nothing but a green-out chance.
     */
    public static void aLoweredLevelCapStillBuysTime(TestContext context) {
        ConfigGameTest.withConfig("{ \"effects\": { \"maxLevel\": 1 } }", () -> {
            ServerPlayerEntity piper = freshPlayer(context);
            smokeUntilClean(context, piper, ModItems.WOODEN_PIPE, ModStrains.INDICA, 2);
            context.assertEquals(0, level(piper, StatusEffects.RESISTANCE), "maxLevel 1 holds Resistance at I");
            context.assertEquals(0, level(piper, StatusEffects.MINING_FATIGUE), "and the cost with it");
            context.assertEquals(EffectPolicy.duration(1050), piper.getStatusEffect(StatusEffects.RESISTANCE).getDuration(),
                    "a second bud past the lowered cap buys half the pipe's duration again");

            // Pairing lifts maxBuffLevel, never maxLevel, so a paired bud turns into time there too.
            List<StatusEffectInstance> paired = hit(new SmokeContents(List.of(
                    new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                    new SmokeContents.Entry(strain(context, ModStrains.HASHISH), 1))));
            context.assertEquals(0, find(paired, StatusEffects.SPEED).getAmplifier(), "maxLevel 1 holds paired Speed at I");
            context.assertEquals(EffectPolicy.duration(1350), find(paired, StatusEffects.SPEED).getDuration(),
                    "a paired second bud past the lowered cap buys time as well");
        });
        context.complete();
    }

    public static void aFullGreenOutEndsTheHigh(TestContext context) {
        ServerPlayerEntity full = highPlayer(context);
        Smoking.greenOut(full, 3);
        context.assertFalse(full.hasStatusEffect(StatusEffects.SPEED), "a smoked buff ends");
        context.assertFalse(full.hasStatusEffect(StatusEffects.ABSORPTION), "an edible's buff ends");
        context.assertTrue(full.hasStatusEffect(StatusEffects.FIRE_RESISTANCE),
                "a potion of a type the mod never grants is left alone");
        context.assertEquals(0f, full.getHungerManager().getSaturationLevel(), "the saturation goes");
        context.assertEquals(600, full.getStatusEffect(StatusEffects.SLOWNESS).getDuration(), "thirty seconds of it");

        // The spins take nothing away.
        ServerPlayerEntity spins = highPlayer(context);
        Smoking.greenOut(spins, 2);
        context.assertTrue(spins.hasStatusEffect(StatusEffects.SPEED), "the spins leave the high running");
        context.assertTrue(spins.hasStatusEffect(StatusEffects.ABSORPTION), "and the edible's");
        context.assertEquals(5f, spins.getHungerManager().getSaturationLevel(), "and the saturation");
        context.assertEquals(300, spins.getStatusEffect(StatusEffects.SLOWNESS).getDuration(), "fifteen seconds of it");
        context.complete();
    }

    public static void aBudWithAConcentrateReachesThree(TestContext context) {
        // Two Lemon Haze and a pinch of hash: the bud's buffs one past the cap, its costs at the dose,
        // and the hash's own Resistance at its own count.
        List<StatusEffectInstance> paired = hit(new SmokeContents(List.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                new SmokeContents.Entry(strain(context, ModStrains.HASHISH), 1))));
        context.assertEquals(2, find(paired, StatusEffects.SPEED).getAmplifier(), "paired Speed reaches III");
        context.assertEquals(2, find(paired, StatusEffects.HASTE).getAmplifier(), "paired Haste reaches III");
        context.assertEquals(1, find(paired, StatusEffects.WEAKNESS).getAmplifier(), "the cost is the bud's dose, not raised");
        context.assertEquals(0, find(paired, StatusEffects.RESISTANCE).getAmplifier(), "the hash's own buff is its own count");

        // Once: three buds and a whole rosin bowl still stop at III, and the rosin at the plain cap.
        List<StatusEffectInstance> heavy = hit(new SmokeContents(List.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 3),
                new SmokeContents.Entry(strain(context, ModStrains.ROSIN), 3))));
        context.assertEquals(2, find(heavy, StatusEffects.SPEED).getAmplifier(), "the bonus is once, not per concentrate");
        context.assertEquals(1, find(heavy, StatusEffects.RESISTANCE).getAmplifier(), "rosin's own Resistance stays at II");

        // Scorched hemp is no concentrate: no buff of its own, so it lifts nothing.
        List<StatusEffectInstance> scorched = hit(new SmokeContents(List.of(
                new SmokeContents.Entry(strain(context, ModStrains.SATIVA), 2),
                new SmokeContents.Entry(strain(context, ModStrains.SCORCHED_HEMP), 1))));
        context.assertEquals(1, find(scorched, StatusEffects.SPEED).getAmplifier(), "scorched hemp does not pair");

        // And the hash family's Hunger scales like the plant's.
        List<StatusEffectInstance> rosin = hit(SmokeContents.of(strain(context, ModStrains.ROSIN), 3));
        context.assertEquals(2, find(rosin, StatusEffects.HUNGER).getAmplifier(), "a rosin bowl is Hunger III");
        context.complete();
    }

    public static void ediblesBuyTimeNotLevel(TestContext context) {
        List<StatusEffectInstance> perfect = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 4, Quality.PERFECT, 0));
        int full = EdibleEffects.durationTicks(EdibleBundle.BUILT_IN, Quality.PERFECT);
        context.assertEquals(1, find(perfect, StatusEffects.ABSORPTION).getAmplifier(), "Absorption stops at II");
        context.assertEquals(0, find(perfect, StatusEffects.RESISTANCE).getAmplifier(), "Resistance I at tier IV");
        context.assertEquals(0, find(perfect, StatusEffects.REGENERATION).getAmplifier(), "Regeneration I");
        context.assertEquals(0, find(perfect, StatusEffects.SLOWNESS).getAmplifier(), "Perfect is Slowness I");
        context.assertEquals(full, find(perfect, StatusEffects.SLOWNESS).getDuration(), "tier IV runs the full time");
        context.assertTrue(find(perfect, StatusEffects.HUNGER).getDuration() > full / 2,
                "the munchies last the high, not a minute");

        List<StatusEffectInstance> rough = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 4, Quality.ROUGH, 0));
        context.assertEquals(1, find(rough, StatusEffects.SLOWNESS).getAmplifier(), "a Rough tier IV is Slowness II");

        List<StatusEffectInstance> weak = allowed(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 1, Quality.PERFECT, 0));
        context.assertEquals(full * 5 / 8, find(weak, StatusEffects.SLOWNESS).getDuration(),
                "tier I lasts five eighths of tier IV");
        context.assertEquals(0, find(weak, StatusEffects.ABSORPTION).getAmplifier(), "tier I is Absorption I");
        context.complete();
    }

    // ---------------------------------------------------------------------

    private static ServerPlayerEntity freshPlayer(TestContext context) {
        return context.createMockCreativeServerPlayerInWorld();
    }

    private static ServerPlayerEntity highPlayer(TestContext context) {
        ServerPlayerEntity player = freshPlayer(context);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 1000, 0));
        player.getHungerManager().setSaturationLevel(5f);
        return player;
    }

    /** What one hit of {@code contents} applies after the config, the way Smoking hands it over. */
    private static List<StatusEffectInstance> hit(SmokeContents contents) {
        return EffectPolicy.filter(contents.effects(900, false, ThreadLocalRandom.current()), contents.buffBonus());
    }

    private static RegistryEntry<Strain> strain(TestContext context, RegistryKey<Strain> key) {
        return context.getWorld().getRegistryManager().getOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static ItemStack packed(TestContext context, Item device, RegistryKey<Strain> strain, int dose) {
        RegistryEntry<Strain> entry = strain(context, strain);
        ItemStack stack = new ItemStack(device);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(entry, dose));
        stack.set(ModComponents.CHARGES, 4);
        return stack;
    }

    private static void smoke(TestContext context, ServerPlayerEntity player, ItemStack stack) {
        player.setStackInHand(Hand.MAIN_HAND, stack);
        stack.getItem().use(context.getWorld(), player, Hand.MAIN_HAND);
    }

    private static void smokeUntilClean(TestContext context, ServerPlayerEntity player, Item device,
                                        RegistryKey<Strain> strain, int dose) {
        for (int i = 0; i < TRIES; i++) {
            player.clearStatusEffects();
            player.getItemCooldownManager().remove(Registries.ITEM.getId(device));
            smoke(context, player, packed(context, device, strain, dose));
            if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                return;
            }
        }
        context.throwGameTestException("never a clean hit in " + TRIES);
    }

    private static int level(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance instance = player.getStatusEffect(effect);
        return instance == null ? -1 : instance.getAmplifier();
    }

    private static List<StatusEffectInstance> allowed(List<EdibleEffects.Dose> bundle) {
        return EffectPolicy.filterKeepingDuration(bundle.stream().map(EdibleEffects.Dose::effect).toList());
    }

    private static StatusEffectInstance find(List<StatusEffectInstance> effects, RegistryEntry<StatusEffect> type) {
        return effects.stream().filter(e -> e.getEffectType().equals(type)).findFirst()
                .orElseThrow(() -> new AssertionError("no " + type.getIdAsString() + " in the bundle"));
    }
}
