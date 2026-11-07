package com.warlonmhite.hempdustry.advancement;

import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.Optional;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Fires whenever a player takes a hit, regardless of the device (spliff/pipe/bong) or strain.
 * Triggered from {@link com.warlonmhite.hempdustry.item.custom.Smoking#takeHit}, not from any item's
 * {@code use} — so it's genuinely "you smoked", independent of which item caused it.
 *
 * <p>The trigger passes the current time of day (already reduced mod 24000); the conditions decide
 * whether it counts. With no window it's strain- and time-agnostic (backs "First Contact"); with a
 * window it only counts inside a time-of-day band (backs the hidden "Blaze It!").
 *
 * <p>A {@code strain} narrows it to a load holding that strain — in <em>any</em> entry, so a moon
 * rock counts for its bud and for its coat alike. It names a key in the datapack strain registry,
 * so a strain a pack adds can be asked for exactly as the mod's own are.
 */
public class SmokeCriterion extends SimpleCriterionTrigger<SmokeCriterion.Conditions> {
    static final ResourceLocation ID = new ResourceLocation(Hempdustry.MOD_ID, "smoke");

    public void trigger(ServerPlayer player, long timeOfDay, ItemStack stack) {
        this.trigger(player, conditions -> conditions.matches(timeOfDay, stack));
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Conditions createInstance(JsonObject json, EntityPredicate.Composite player, DeserializationContext context) {
        return new Conditions(player,
                json.has("item") ? Optional.of(ItemPredicate.fromJson(json.get("item"))) : Optional.empty(),
                json.has("time") ? Optional.of(TimeWindow.fromJson(GsonHelper.getAsJsonObject(json, "time"))) : Optional.empty(),
                json.has("strain")
                        ? Optional.of(ResourceKey.create(Strain.REGISTRY_KEY, new ResourceLocation(GsonHelper.getAsString(json, "strain"))))
                        : Optional.empty());
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final Optional<ItemPredicate> item;
        private final Optional<TimeWindow> time;
        private final Optional<ResourceKey<Strain>> strain;

        public Conditions(EntityPredicate.Composite player, Optional<ItemPredicate> item, Optional<TimeWindow> time,
                          Optional<ResourceKey<Strain>> strain) {
            super(ID, player);
            this.item = item;
            this.time = time;
            this.strain = strain;
        }

        public Optional<ItemPredicate> item() {
            return item;
        }

        public Optional<TimeWindow> time() {
            return time;
        }

        public Optional<ResourceKey<Strain>> strain() {
            return strain;
        }

        public boolean matches(long timeOfDay, ItemStack stack) {
            if (time.isPresent() && !time.get().contains(timeOfDay)) {
                return false;
            }
            if (strain.isPresent() && ModComponents.SMOKE_CONTENTS.getOrDefault(stack, SmokeContents.EMPTY)
                    .entries().stream().noneMatch(entry -> entry.strain().is(strain.get()))) {
                return false;
            }
            return item.isEmpty() || item.get().matches(stack);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            item.ifPresent(predicate -> json.add("item", predicate.serializeToJson()));
            time.ifPresent(window -> json.add("time", window.toJson()));
            strain.ifPresent(key -> json.addProperty("strain", key.location().toString()));
            return json;
        }

        public static Conditions any() {
            return new Conditions(EntityPredicate.Composite.ANY, Optional.empty(), Optional.empty(), Optional.empty());
        }

        public static Conditions of(ResourceKey<Strain> strain) {
            return new Conditions(EntityPredicate.Composite.ANY, Optional.empty(), Optional.empty(), Optional.of(strain));
        }

        public static Conditions with(ItemLike... items) {
            return new Conditions(EntityPredicate.Composite.ANY, Optional.of(ItemPredicate.Builder.item().of(items).build()),
                    Optional.empty(), Optional.empty());
        }

        public static Conditions during(long minTicks, long maxTicks) {
            return new Conditions(EntityPredicate.Composite.ANY, Optional.empty(),
                    Optional.of(new TimeWindow(minTicks, maxTicks)), Optional.empty());
        }
    }

    /** An inclusive time-of-day band, in ticks within a 0–23999 vanilla day. */
    public record TimeWindow(long min, long max) {
        static TimeWindow fromJson(JsonObject json) {
            return new TimeWindow(GsonHelper.getAsLong(json, "min"), GsonHelper.getAsLong(json, "max"));
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("min", min);
            json.addProperty("max", max);
            return json;
        }

        public boolean contains(long timeOfDay) {
            return timeOfDay >= min && timeOfDay <= max;
        }
    }
}
