package com.warlonmhite.hempdustry.advancement;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import java.util.Optional;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

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
 *
 * <p><b>What it can filter on</b>, each optional: the number of {@code cuts}; the {@code condition} the
 * plant kept a record of (read off the plant, never off the lamp, so it cannot be cheated at the
 * last moment); and the {@code bed} it grew in, a vanilla {@link BlockPredicate} tested against the
 * block under the LOWER segment — so a datapack can ask for a Hydro Tray, a Grow Pot, or a tray that
 * was fed, in the same vocabulary as {@code minecraft:location}.
 */
public class HarvestHempCriterion extends SimpleCriterionTrigger<HarvestHempCriterion.Conditions> {
    static final ResourceLocation ID = new ResourceLocation(Hempdustry.MOD_ID, "harvest_hemp");

    public static void trigger(Player player, ServerLevel world, BlockPos lowerPos, BlockState lowerState) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !lowerState.hasProperty(Defoliation.TRIMMED_EARLY)) {
            return;
        }
        int cuts = Defoliation.cutCount(lowerState);
        Optional<GrowLight> light = lowerState.getOptionalValue(GrowLight.PROPERTY);
        BlockPos bedPos = lowerPos.below();
        ModCriteria.HARVEST_HEMP.trigger(serverPlayer,
                conditions -> conditions.matches(cuts, light, world, bedPos));
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Conditions createInstance(JsonObject json, EntityPredicate.Composite player, DeserializationContext context) {
        Optional<GrowLight> light = Optional.empty();
        if (json.has("condition")) {
            String name = GsonHelper.getAsString(json, "condition");
            light = Optional.of(java.util.Arrays.stream(GrowLight.values())
                    .filter(value -> value.getSerializedName().equals(name)).findFirst()
                    .orElseThrow(() -> new JsonSyntaxException("Unknown condition '" + name + "'")));
        }
        return new Conditions(player,
                MinMaxBounds.Ints.fromJson(json.get("cuts")),
                light,
                json.has("bed") ? Optional.of(BlockPredicate.fromJson(json.get("bed"))) : Optional.empty());
    }

    /**
     * The optional fields are additive: a criterion written before {@code condition} and {@code bed}
     * existed decodes with both empty and means exactly what it meant.
     */
    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final MinMaxBounds.Ints cuts;
        private final Optional<GrowLight> light;
        private final Optional<BlockPredicate> bed;

        public Conditions(EntityPredicate.Composite player, MinMaxBounds.Ints cuts, Optional<GrowLight> light,
                          Optional<BlockPredicate> bed) {
            super(ID, player);
            this.cuts = cuts;
            this.light = light;
            this.bed = bed;
        }

        public MinMaxBounds.Ints cuts() {
            return cuts;
        }

        public Optional<GrowLight> light() {
            return light;
        }

        public Optional<BlockPredicate> bed() {
            return bed;
        }

        public boolean matches(int cuts, Optional<GrowLight> light, ServerLevel world, BlockPos bedPos) {
            return this.cuts.matches(cuts)
                    && (!this.light.isPresent() || this.light.equals(light))
                    && (!this.bed.isPresent() || this.bed.get().matches(world, bedPos));
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (!cuts.isAny()) {
                json.add("cuts", cuts.serializeToJson());
            }
            light.ifPresent(value -> json.addProperty("condition", value.getSerializedName()));
            bed.ifPresent(predicate -> json.add("bed", predicate.serializeToJson()));
            return json;
        }

        public static Conditions fullyTrimmed() {
            return create(MinMaxBounds.Ints.exactly(2), Optional.empty(), Optional.empty());
        }

        public static Conditions grownUnder(GrowLight light) {
            return create(MinMaxBounds.Ints.ANY, Optional.of(light), Optional.empty());
        }

        public static Conditions grownIn(BlockPredicate.Builder bed) {
            return create(MinMaxBounds.Ints.ANY, Optional.empty(), Optional.of(bed.build()));
        }

        public static Conditions create(MinMaxBounds.Ints cuts, Optional<GrowLight> light, Optional<BlockPredicate> bed) {
            return new Conditions(EntityPredicate.Composite.ANY, cuts, light, bed);
        }
    }
}
