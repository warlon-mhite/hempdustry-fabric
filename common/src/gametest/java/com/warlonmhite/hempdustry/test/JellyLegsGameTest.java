package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.custom.JellyLegs;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * A green-out softens the legs for as long as it slows the player, and nothing else does. Read through
 * the attributes the game moves the player by, so a modifier that stopped landing (or stopped leaving)
 * fails here and not only in a client.
 */
public final class JellyLegsGameTest {

    public static void aGreenOutSoftensTheLegsWhileItSlows(GameTestHelper context) {
        // Slowness of any other origin, a potion's say, leaves the legs alone.
        ServerPlayer potion = Players.survival(context);
        potion.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 1));
        context.assertFalse(JellyLegs.isSoft(potion), "a potion of Slowness softened the legs");

        ServerPlayer player = Players.survival(context);
        double steady = player.getAttributeValue(Attributes.FRICTION_MODIFIER);
        Smoking.greenOut(player, 2);
        context.assertTrue(JellyLegs.isSoft(player), "the spins did not soften the legs");
        double sliding = player.getAttributeValue(Attributes.FRICTION_MODIFIER);
        context.assertTrue(Math.abs(sliding - steady * 0.6) < 1.0E-9,
                "a green-out's friction is " + sliding + ", not 0.6 of a steady player's " + steady);
        // By the modifier, not the value: NeoForge 26.2.0.93 floors knockback resistance at 0.
        context.assertTrue(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).hasModifier(JellyLegs.ID),
                "a green-out did not make the player easier to knock back");

        // The full green-out softens them too.
        ServerPlayer full = Players.survival(context);
        Smoking.greenOut(full, 3);
        context.assertTrue(JellyLegs.isSoft(full), "the full green-out did not soften the legs");

        // They firm up once the player is no longer slowed.
        player.removeEffect(MobEffects.SLOWNESS);
        context.succeedWhen(() -> {
            context.assertFalse(JellyLegs.isSoft(player), "the legs stayed soft after the Slowness ended");
            context.assertFalse(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).hasModifier(JellyLegs.ID),
                    "the knockback stayed heavy after the Slowness ended");
            context.assertTrue(player.getAttributeValue(Attributes.FRICTION_MODIFIER) == steady,
                    "the friction did not come back to " + steady);
            context.assertTrue(JellyLegs.isSoft(full), "the full green-out's legs firmed up while it still slowed");
        });
    }
}
