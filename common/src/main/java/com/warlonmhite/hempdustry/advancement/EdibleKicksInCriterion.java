package com.warlonmhite.hempdustry.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fires when an edible kicks in: the moment its first effect lands, after the come-up, not on the
 * swallow. Triggered from {@link com.warlonmhite.hempdustry.item.custom.EdibleScheduler}, so every
 * edible counts, whichever door it was eaten through.
 *
 * <p>{@code cbd_dominant} narrows it to an edible with more CBD than THC in it — a higher CBD tier
 * than THC tier, which is what its tooltip shows. The {@code player} field is vanilla's, so "while
 * greened out" is the player's own effects, asked the way any advancement asks them.
 */
public class EdibleKicksInCriterion extends SimpleCriterionTrigger<EdibleKicksInCriterion.Conditions> {

    /** Call from the server side as an edible's first effect lands. */
    public void trigger(ServerPlayer player, boolean cbdDominant) {
        this.trigger(player, conditions -> conditions.matches(cbdDominant));
    }

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<ContextAwarePredicate> player, Optional<Boolean> cbdDominant)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.BOOL.optionalFieldOf("cbd_dominant").forGetter(Conditions::cbdDominant)
        ).apply(instance, Conditions::new));

        public boolean matches(boolean cbdDominant) {
            return this.cbdDominant.isEmpty() || this.cbdDominant.get() == cbdDominant;
        }

        /** A CBD-dominant edible kicking in, for a player {@code player} holds for. */
        public static Criterion<Conditions> cbdDominant(EntityPredicate.Builder player) {
            return ModCriteria.EDIBLE_KICKS_IN.createCriterion(new Conditions(
                    Optional.of(EntityPredicate.wrap(player)), Optional.of(true)));
        }
    }
}
