package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * A green-out takes the ground out from under you: for as long as its Slowness lasts, the player's
 * feet slide when they stop and a hit sends them further. Fights and cliff edges become a risk;
 * standing still is safe.
 *
 * <p>Two of 26.2's movement attributes, the ones sulfur cubes are built on, and both on every living
 * entity. {@code friction_modifier} scales how much of a block's slipperiness is taken away:
 * {@code LivingEntity#computeModifiedFriction} is {@code 1 - (1 - friction) * modifier}, so at 0.6
 * stone's 0.6 reads 0.76, a third of the way to ice's 0.98. The attribute is synced, which matters:
 * a player's own movement is worked out on their client. {@code knockback_resistance} at -0.5 makes
 * knockback half as strong again ({@code LivingEntity#knockback} multiplies by {@code 1 - resistance});
 * NeoForge 26.2.0.93 still floors that attribute at 0, so there only the sliding is felt.
 *
 * <p><b>Riding on the green-out's Slowness rather than a status effect of its own</b>, so there is no
 * new effect id to freeze and nothing new on the HUD: the legs go soft when the green-out lands and
 * firm up the tick the player is no longer slowed, milk included. The modifiers are transient, so
 * they are never written to the player's save: a world opened without the mod cannot keep a player
 * sliding for ever, and a rejoin simply ends it.
 */
public final class JellyLegs {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "jelly_legs");

    /** Ground friction taken away: the modifier ends at 0.6 times a steady player's. */
    public static final double FRICTION = -0.4;
    /** Added to knockback resistance: a hit lands half as hard again. */
    public static final double KNOCKBACK_RESISTANCE = -0.5;

    private static final AttributeModifier SLIDE =
            new AttributeModifier(ID, FRICTION, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final AttributeModifier PUSHED =
            new AttributeModifier(ID, KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE);

    private JellyLegs() {
    }

    /** Registers the tick that firms the legs up again. Call once from mod init. */
    public static void init() {
        Services.EVENTS.onServerTickEnd(JellyLegs::tick);
    }

    /**
     * Softens {@code player}'s legs, if the green-out just slowed them; a green-out whose Slowness a
     * datapack or the config took away leaves the legs alone. Legible on purpose, since it can put a
     * player off a cliff: a squelch and a puff of slime at their feet.
     */
    public static void soften(Player player) {
        if (!player.hasEffect(MobEffects.SLOWNESS)) {
            return;
        }
        modifier(player, Attributes.FRICTION_MODIFIER, SLIDE);
        modifier(player, Attributes.KNOCKBACK_RESISTANCE, PUSHED);
        if (player.level() instanceof ServerLevel world) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.8F, 0.8F);
            world.sendParticles(ParticleTypes.ITEM_SLIME, player.getX(), player.getY() + 0.1, player.getZ(),
                    12, 0.3, 0.05, 0.3, 0.0);
        }
    }

    /** Whether {@code player}'s legs are soft right now. */
    public static boolean isSoft(Player player) {
        AttributeInstance friction = player.getAttribute(Attributes.FRICTION_MODIFIER);
        return friction != null && friction.hasModifier(ID);
    }

    private static void modifier(Player player, Holder<Attribute> attribute, AttributeModifier modifier) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(modifier);
        }
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isSoft(player) && !player.hasEffect(MobEffects.SLOWNESS)) {
                player.getAttribute(Attributes.FRICTION_MODIFIER).removeModifier(ID);
                AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (knockback != null) {
                    knockback.removeModifier(ID);
                }
            }
        }
    }
}
