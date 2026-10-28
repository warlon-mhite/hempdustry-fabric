package com.warlonmhite.hempdustry.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import java.util.Optional;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
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

    /**
     * Call from the server side when a mature plant is broken, having resolved it down to its LOWER
     * segment (which is the only one carrying {@code AGE}, the trim flags and the light record).
     * Call it <b>before</b> the plant is removed: the bed is read from under it.
     *
     * <p>Silently does nothing for a non-player or for a state without the trim properties, so a
     * caller can hand it whatever it has resolved without pre-checking.
     */
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
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    /** The record's own names ({@code "grow_lamp"}, {@code "stressed"}…), as the blockstate spells them. */
    private static final Codec<GrowLight> LIGHT_CODEC = StringRepresentable.fromEnum(GrowLight::values);

    /**
     * The optional fields are additive: a criterion written before {@code condition} and {@code bed}
     * existed decodes with both empty and means exactly what it meant.
     */
    public record Conditions(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints cuts,
                             Optional<GrowLight> light, Optional<BlockPredicate> bed)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("cuts", MinMaxBounds.Ints.ANY).forGetter(Conditions::cuts),
                LIGHT_CODEC.optionalFieldOf("condition").forGetter(Conditions::light),
                BlockPredicate.CODEC.optionalFieldOf("bed").forGetter(Conditions::bed)
        ).apply(instance, Conditions::new));

        /** A plant with no light record (a crop without the property) never matches a light filter. */
        public boolean matches(int cuts, Optional<GrowLight> light, ServerLevel world, BlockPos bedPos) {
            return this.cuts.matches(cuts)
                    && (this.light.isEmpty() || this.light.equals(light))
                    && (this.bed.isEmpty() || this.bed.get().matches(world, bedPos));
        }

        /** Only counts a harvest of a plant cut in <b>both</b> windows. */
        public static Criterion<Conditions> fullyTrimmed() {
            return create(MinMaxBounds.Ints.exactly(2), Optional.empty(), Optional.empty());
        }

        /** Only counts a plant that kept {@code light} as its {@code condition} all the way to harvest. */
        public static Criterion<Conditions> grownUnder(GrowLight light) {
            return create(MinMaxBounds.Ints.ANY, Optional.of(light), Optional.empty());
        }

        /** Only counts a plant whose bed — the block under it — matches {@code bed}. */
        public static Criterion<Conditions> grownIn(BlockPredicate.Builder bed) {
            return create(MinMaxBounds.Ints.ANY, Optional.empty(), Optional.of(bed.build()));
        }

        public static Criterion<Conditions> create(MinMaxBounds.Ints cuts, Optional<GrowLight> light,
                                                              Optional<BlockPredicate> bed) {
            return ModCriteria.HARVEST_HEMP.createCriterion(new Conditions(Optional.empty(), cuts, light, bed));
        }
    }
}
