package com.warlonmhite.hempdustry.test;

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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;

import java.util.List;

/**
 * The 2.0.1 rebalance, through the real hit where it can be: {@code Item#use} on a packed device, so
 * the strain JSON, {@code SmokeContents}, {@code Smoking} and {@code EffectPolicy} all have their say.
 *
 * <p>A green-out is a roll this test cannot seed, so a bong of three is smoked until it has seen
 * both a clean hit and a green-out. At one in four, sixty hits without a green-out is about three in
 * a hundred million.
 */
public final class RebalanceGameTest implements FabricGameTest {
    private static final int TRIES = 60;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void aBigDoseBuysTimeNotLevel(TestContext context) {
        ServerPlayerEntity clean = null;
        boolean sawGreenOut = false;
        for (int i = 0; i < TRIES && (clean == null || !sawGreenOut); i++) {
            ServerPlayerEntity player = freshPlayer(context);
            player.getHungerManager().setSaturationLevel(5f);
            ItemStack bong = packed(context, ModItems.BONG, ModStrains.SATIVA, 3);
            smoke(context, player, bong);
            if (player.hasStatusEffect(StatusEffects.SPEED)) {
                clean = player;
            } else {
                sawGreenOut = true;
                long lockout = bong.getOrDefault(ModComponents.COOLDOWN_UNTIL, 0L) - context.getWorld().getTime();
                context.assertEquals((long) EffectPolicy.cooldown(Smoking.GREEN_OUT_LOCKOUT_TICKS), lockout,
                        "a full green-out locks smoking out for its minute, not the bong's own cooldown");
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
        context.assertEquals(EffectPolicy.duration(1500), clean.getStatusEffect(StatusEffects.SPEED).getDuration(),
                "the third bud buys half the bong's duration again");

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
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void aLoweredLevelCapStillBuysTime(TestContext context) {
        ConfigGameTest.withConfig("{ \"effects\": { \"maxLevel\": 1 } }", () -> {
            ServerPlayerEntity piper = freshPlayer(context);
            smokeUntilClean(context, piper, ModItems.WOODEN_PIPE, ModStrains.INDICA, 2);
            context.assertEquals(0, level(piper, StatusEffects.RESISTANCE), "maxLevel 1 holds Resistance at I");
            context.assertEquals(0, level(piper, StatusEffects.MINING_FATIGUE), "and the cost with it");
            context.assertEquals(EffectPolicy.duration(1050), piper.getStatusEffect(StatusEffects.RESISTANCE).getDuration(),
                    "a second bud past the lowered cap buys half the pipe's duration again");
        });
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void aFullGreenOutEndsTheHigh(TestContext context) {
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void ediblesBuyTimeNotLevel(TestContext context) {
        List<StatusEffectInstance> perfect = allowed(EdibleEffects.bundle(4, Quality.PERFECT, 0));
        int full = EdibleEffects.durationTicks(Quality.PERFECT);
        context.assertEquals(1, find(perfect, StatusEffects.ABSORPTION).getAmplifier(), "Absorption stops at II");
        context.assertEquals(0, find(perfect, StatusEffects.RESISTANCE).getAmplifier(), "Resistance I at tier IV");
        context.assertEquals(0, find(perfect, StatusEffects.REGENERATION).getAmplifier(), "Regeneration I");
        context.assertEquals(0, find(perfect, StatusEffects.SLOWNESS).getAmplifier(), "Perfect is Slowness I");
        context.assertEquals(full, find(perfect, StatusEffects.SLOWNESS).getDuration(), "tier IV runs the full time");
        context.assertTrue(find(perfect, StatusEffects.HUNGER).getDuration() > full / 2,
                "the munchies last the high, not a minute");

        List<StatusEffectInstance> rough = allowed(EdibleEffects.bundle(4, Quality.ROUGH, 0));
        context.assertEquals(1, find(rough, StatusEffects.SLOWNESS).getAmplifier(), "a Rough tier IV is Slowness II");

        List<StatusEffectInstance> weak = allowed(EdibleEffects.bundle(1, Quality.PERFECT, 0));
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

    private static ItemStack packed(TestContext context, Item device, RegistryKey<Strain> strain, int dose) {
        RegistryEntry<Strain> entry = context.getWorld().getRegistryManager()
                .get(Strain.REGISTRY_KEY).entryOf(strain);
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
            player.getItemCooldownManager().remove(device);
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
