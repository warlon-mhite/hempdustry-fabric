package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * What 2.0 let an addon call on a strain and a load, still answering as 2.0 did.
 *
 * <p>{@code scripts/api_additive.py} proves every 2.0 member still links; this proves the bridges
 * say the right thing. 2.0 had no strain without a plant, no effect held to the exhale and no effect
 * that might not land, so a bridge has to decide what each of those looks like in 2.0's terms.
 */
@SuppressWarnings("deprecation")
public final class ApiBridgeGameTest {

    public static void theTwoZeroApiStillAnswers(GameTestHelper context) {
        Registry<Strain> strains = context.getLevel().registryAccess().lookupOrThrow(Strain.REGISTRY_KEY);
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
                MobEffects.BLINDNESS, "Beldía's exhale Blindness, from Strain#effects(int, int)");
        assertHas(context, strains.getOrThrow(ModStrains.SCHWAG).value().effects(1, 100),
                MobEffects.POISON, "schwag's 60% Poison, from Strain#effects(int, int)");
        assertHas(context, SmokeContents.of(strains.getOrThrow(ModStrains.BELDIA), 1).effects(100),
                MobEffects.BLINDNESS, "Beldía's exhale Blindness, from SmokeContents#effects(int)");
        assertHas(context, SmokeContents.of(strains.getOrThrow(ModStrains.SCHWAG), 1).effects(100),
                MobEffects.POISON, "schwag's 60% Poison, from SmokeContents#effects(int)");
        context.succeed();
    }

    private static void assertHas(GameTestHelper context, List<MobEffectInstance> effects,
                                  Holder<MobEffect> effect, String what) {
        ResourceKey<MobEffect> key = effect.unwrapKey().orElseThrow();
        context.assertTrue(effects.stream().anyMatch(instance -> instance.getEffect().is(key)),
                what + " is missing: " + effects);
    }
}
