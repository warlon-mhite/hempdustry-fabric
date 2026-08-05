package com.warlonmhite.hempdustry.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Fires when a player harvests a <em>mature</em> hemp plant, carrying how many of the two
 * defoliation windows that plant was cut in. Strain-agnostic, like {@link SmokeCriterion}: both
 * crops route through {@link #trigger}, and a third would too.
 *
 * <p>This needs to be a custom criterion because nothing in vanilla can see it. The payout of a
 * fully-trimmed plant is a different <em>quantity</em> of the same items, which no inventory check
 * can distinguish, and the two trim flags live in the block state rather than on anything the
 * player holds.
 *
 * <p><b>Why the harvest and not the second cut.</b> Making the late cut and reaping what it bought
 * are two different moments, and the reaping is the one the player can see — four buds instead of
 * two. It is also the robust one: {@code minecraft:item_used_on_block} reports the <em>clicked</em>
 * position, which for a two- or three-tall plant may be a segment that doesn't carry the flags at
 * all, so a state predicate on the trim booleans would only fire when the player happened to shear
 * the bottom block.
 */
public class HarvestHempCriterion extends AbstractCriterion<HarvestHempCriterion.Conditions> {

    /**
     * Call from the server side when a mature plant is broken, having resolved it down to its LOWER
     * segment (which is the only one carrying {@code AGE} and the trim flags).
     *
     * <p>Silently does nothing off-server, for a non-player, or for a state without the trim
     * properties, so a caller can hand it whatever it has resolved without pre-checking.
     */
    public static void trigger(PlayerEntity player, BlockState lowerState) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !lowerState.contains(Defoliation.TRIMMED_EARLY)) {
            return;
        }
        int cuts = Defoliation.cutCount(lowerState);
        ModCriteria.HARVEST_HEMP.trigger(serverPlayer, conditions -> conditions.matches(cuts));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<LootContextPredicate> player, NumberRange.IntRange cuts)
            implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                NumberRange.IntRange.CODEC.optionalFieldOf("cuts", NumberRange.IntRange.ANY).forGetter(Conditions::cuts)
        ).apply(instance, Conditions::new));

        public boolean matches(int cuts) {
            return this.cuts.test(cuts);
        }

        /** Only counts a harvest of a plant cut in <b>both</b> windows. */
        public static AdvancementCriterion<Conditions> fullyTrimmed() {
            return ModCriteria.HARVEST_HEMP.create(
                    new Conditions(Optional.empty(), NumberRange.IntRange.exactly(2)));
        }
    }
}
