package com.warlonmhite.hempdustry.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Fires whenever a player takes a hit, regardless of the device (spliff/pipe/bong) or strain.
 * Triggered from {@link com.warlonmhite.hempdustry.item.custom.Smoking#takeHit}, not from any item's
 * {@code use} — so it's genuinely "you smoked", independent of which item caused it.
 *
 * <p>The trigger passes the current time of day (already reduced mod 24000); the conditions decide
 * whether it counts. With no window it's strain- and time-agnostic (backs "First Contact"); with a
 * window it only counts inside a time-of-day band (backs the hidden "Blaze It!").
 */
public class SmokeCriterion extends AbstractCriterion<SmokeCriterion.Conditions> {

    /**
     * Call from the server side after a hit lands.
     *
     * @param timeOfDay the world's time of day, already reduced to {@code getTimeOfDay() % 24000}
     *                  (tick 0 = 6:00 AM, 1000 ticks per in-game hour).
     */
    public void trigger(ServerPlayerEntity player, long timeOfDay) {
        this.trigger(player, conditions -> conditions.matches(timeOfDay));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<LootContextPredicate> player, Optional<TimeWindow> time)
            implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                TimeWindow.CODEC.optionalFieldOf("time").forGetter(Conditions::time)
        ).apply(instance, Conditions::new));

        /** True when there's no time window, or the given time of day falls inside it. */
        public boolean matches(long timeOfDay) {
            return time.isEmpty() || time.get().contains(timeOfDay);
        }

        /** No conditions — any player, any device, any strain, any time. */
        public static AdvancementCriterion<Conditions> any() {
            return ModCriteria.SMOKE.create(new Conditions(Optional.empty(), Optional.empty()));
        }

        /** Only counts a hit taken while the time of day is within {@code [minTicks, maxTicks]} (inclusive). */
        public static AdvancementCriterion<Conditions> during(long minTicks, long maxTicks) {
            return ModCriteria.SMOKE.create(new Conditions(Optional.empty(), Optional.of(new TimeWindow(minTicks, maxTicks))));
        }
    }

    /** An inclusive time-of-day band, in ticks within a 0–23999 vanilla day. */
    public record TimeWindow(long min, long max) {
        public static final Codec<TimeWindow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("min").forGetter(TimeWindow::min),
                Codec.LONG.fieldOf("max").forGetter(TimeWindow::max)
        ).apply(instance, TimeWindow::new));

        public boolean contains(long timeOfDay) {
            return timeOfDay >= min && timeOfDay <= max;
        }
    }
}
