package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.test.TestContext;

import java.util.List;

/**
 * What 2.0 let an addon call on a strain and a load, still answering as 2.0 did.
 *
 * <p>{@code scripts/api_additive.py} proves every 2.0 member still links; this proves the bridges
 * say the right thing. 2.0 had no strain without a plant, no effect held to the exhale and no effect
 * that might not land, so a bridge has to decide what each of those looks like in 2.0's terms.
 */
@SuppressWarnings("deprecation")
public final class ApiBridgeGameTest {

    public static void theTwoZeroApiStillAnswers(TestContext context) {
        Registry<Strain> strains = context.getWorld().getRegistryManager().getOrThrow(Strain.REGISTRY_KEY);
        Strain indica = strains.getOrThrow(ModStrains.INDICA).value();

        // 2.0's constructor and accessors: a plant strain, with the defaults 2.0 behaved by.
        Strain built = new Strain(indica.translationKey(), indica.color(), indica.modelIndex(),
                indica.seeds(), indica.buds(), indica.flower(), indica.smokeEffects());
        context.assertTrue(built.seedItem().equals(indica.seedItem()) && built.wildFlower().equals(indica.wildFlower()),
                "2.0's constructor lost Purple Kush's seeds or flower: " + built);
        context.assertTrue(built.greenOutFactor() == 1.0F && built.coughFactor() == 1.0F && built.dosePerItem() == 1,
                "2.0's constructor did not take 2.0's defaults: " + built);

        // A strain 2.0 never had, with no plant, answers air rather than null.
        Strain hashish = strains.getOrThrow(ModStrains.HASHISH).value();
        context.assertTrue(hashish.seeds() == Items.AIR && hashish.flower() == Blocks.AIR,
                "hashish's 2.0 seeds()/flower() are " + hashish.seeds() + " / " + hashish.flower() + ", not air");

        // effects(dose, duration) is the whole hit: the exhale's effects and the chance ones too.
        assertHas(context, strains.getOrThrow(ModStrains.BELDIA).value().effects(1, 100),
                StatusEffects.BLINDNESS, "Beldía's exhale Blindness, from Strain#effects(int, int)");
        assertHas(context, strains.getOrThrow(ModStrains.SCHWAG).value().effects(1, 100),
                StatusEffects.POISON, "schwag's 60% Poison, from Strain#effects(int, int)");
        assertHas(context, SmokeContents.of(strains.getOrThrow(ModStrains.BELDIA), 1).effects(100),
                StatusEffects.BLINDNESS, "Beldía's exhale Blindness, from SmokeContents#effects(int)");
        assertHas(context, SmokeContents.of(strains.getOrThrow(ModStrains.SCHWAG), 1).effects(100),
                StatusEffects.POISON, "schwag's 60% Poison, from SmokeContents#effects(int)");
        context.complete();
    }

    private static void assertHas(TestContext context, List<StatusEffectInstance> effects,
                                  RegistryEntry<StatusEffect> effect, String what) {
        RegistryKey<StatusEffect> key = effect.getKey().orElseThrow();
        context.assertTrue(effects.stream().anyMatch(instance -> instance.getEffectType().matchesKey(key)),
                what + " is missing: " + effects);
    }
}
