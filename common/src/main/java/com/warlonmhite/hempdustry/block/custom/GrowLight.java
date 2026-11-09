package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import java.util.function.Function;

/**
 * The light a hemp plant grew under — a <b>record the plant keeps</b>, never a reading of the lamp
 * at harvest. Canonical on the LOWER segment, like the age and the trim flags, and copied onto every
 * segment above it, because whatever draws a block reads only that block.
 *
 * <p>The first growth step writes the tier overhead. Every later step, natural or bone meal, can
 * only <b>lower</b> it to the weakest light the plant has grown under; losing the light entirely
 * turns a flowering plant {@link #STRESSED} (a light-interrupted plant going hermaphrodite and
 * seeding its own buds) and a younger one back to {@link #NATURAL}. So the harvest cannot be
 * cheated by moving a lamp over at the end, one lamp cannot be shuttled between plants (growth
 * steps land at random moments), and swapping a Grow Lamp for glowstone halfway through harvests as
 * glowstone. A bee's step looks too: {@code BeeGrowCropsGoalMixin} runs it through
 * {@link PlantStress#beeStep}.
 *
 * <p>Losing the light is one way to {@link #STRESSED} and the only certain one; the rest — a dry bed,
 * a dead pump, overfeeding, overwatering, a bone-dry field — are rolls, in {@link PlantStress}.
 *
 * <p>The block-state property is {@code condition}, not {@code light}: the record began as the
 * light alone, and {@link #STRESSED} now comes of any of those causes, so the name a resource pack
 * or a datapack reads says what the value means.
 *
 * <p>The constants are in tier order, weakest first, and {@link #afterStep} relies on it.
 */
public enum GrowLight implements StringRepresentable {
    /** Sun, torches, or nothing counted — the baseline. */
    NATURAL("natural"),
    /** Any other block at light 15: glowstone, sea lantern, shroomlight, froglights, lanterns. */
    AMBIENT("ambient"),
    /** A block at light 15 that is switched on: a redstone lamp, a copper bulb, most modded lamps. */
    LAMP("lamp"),
    /** {@code #hempdustry:grow_lamps} — the Grow Lamp, and whatever a pack adds. */
    GROW_LAMP("grow_lamp"),
    /** Lost its lamp while flowering, or stressed some other way ({@link PlantStress}). Harvests a bud short and seeded. */
    STRESSED("stressed");

    public static final EnumProperty<GrowLight> PROPERTY = EnumProperty.create("condition", GrowLight.class);

    /** The age the plant starts flowering at — the age both crops first stand two blocks tall. */
    public static final int FLOWERING_AGE = 4;
    /** Grow Lamp speed while the plant is young: the long days growers give a plant before they flip it to flower. */
    public static final float VEG_SPEED = 1.1F;
    /** How far above the plant's full-grown top a light still counts. */
    private static final int REACH = 3;

    private final String name;

    GrowLight(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * Anything but {@link #NATURAL}: a plant that grew under a light, or one that was stressed. Only
     * these can be stressed by <em>losing</em> a light, and charas rubs at the lamp's odds off all of them.
     */
    public boolean artificial() {
        return this != NATURAL;
    }

    /** The record after one growth step taken from {@code fromAge}, with {@code now} overhead. */
    public GrowLight afterStep(int fromAge, GrowLight now) {
        return this.afterStep(fromAge, now, FLOWERING_AGE);
    }

    /**
     * As {@link #afterStep(int, GrowLight)}, for a plant that a lost light stresses once a step takes
     * it to {@code lossAge} or past: {@link #FLOWERING_AGE} for most, a stage earlier for a plant
     * touchy about its light ({@link PlantStress#TOUCHY_AGE}).
     */
    public GrowLight afterStep(int fromAge, GrowLight now, int lossAge) {
        if (fromAge == 0) {
            return now;
        }
        if (this == NATURAL || this == STRESSED) {
            return this;
        }
        if (now == NATURAL) {
            return fromAge + 1 >= lossAge ? STRESSED : NATURAL;
        }
        return now.ordinal() < this.ordinal() ? now : this;
    }

    /** The speed factor this record gives the growth roll at {@code age}. */
    public float speedAt(int age) {
        return this == GROW_LAMP && age < FLOWERING_AGE ? VEG_SPEED : 1.0F;
    }

    /**
     * The best light over the plant whose LOWER is at {@code lower} and which stands {@code height}
     * blocks tall once grown. A Grow Lamp reaches the 3×3 plants below it; every other light only
     * the plant directly underneath.
     */
    public static GrowLight over(BlockGetter world, BlockPos lower, int height) {
        GrowLight best = NATURAL;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                GrowLight found = scan(world, lower.offset(dx, height, dz));
                if (found == GROW_LAMP) {
                    return GROW_LAMP;
                }
                if (dx == 0 && dz == 0) {
                    best = found;
                }
            }
        }
        return best;
    }

    /**
     * The light hanging over the plant, lit or not: what {@link #over} would read with every lamp
     * there switched on. A dark Grow Lamp reads {@link #GROW_LAMP}, a dark redstone lamp {@link #LAMP}.
     * It is what a plant keeps through a night with its lamp off: the lamp is still there.
     */
    public static GrowLight fixtureOver(BlockGetter world, BlockPos lower, int height) {
        GrowLight best = NATURAL;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                GrowLight found = scan(world, lower.offset(dx, height, dz), GrowLight::ofFixture);
                if (found == GROW_LAMP) {
                    return GROW_LAMP;
                }
                if (dx == 0 && dz == 0) {
                    best = found;
                }
            }
        }
        return best;
    }

    /** Walks up from {@code from}: light passes glass and plants, and an opaque block stops it. */
    private static GrowLight scan(BlockGetter world, BlockPos from) {
        return scan(world, from, GrowLight::of);
    }

    private static GrowLight scan(BlockGetter world, BlockPos from, Function<BlockState, GrowLight> reading) {
        for (int i = 0; i < REACH; i++) {
            BlockState state = world.getBlockState(from.above(i));
            GrowLight tier = reading.apply(state);
            if (tier != NATURAL || state.isSolidRender()) {
                return tier;
            }
        }
        return NATURAL;
    }

    /**
     * What one block counts as. The tag first, so a pack can name a grow light outright; then light
     * 15 decides, and the {@code LIT} property is what tells a lamp from glowstone. Only 15 counts,
     * which keeps torches and end rods (14) out — otherwise every lit room would be a bonus — and
     * lets a copper bulb stop counting as it oxidises. {@code #hempdustry:not_grow_lights} catches
     * what is bright but is not a light over a plant: campfires, fire, lava.
     */
    public static GrowLight of(BlockState state) {
        if (state.is(ModTags.Blocks.GROW_LAMPS)) {
            return !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT) ? GROW_LAMP : NATURAL;
        }
        if (state.getLightEmission() < 15 || state.is(ModTags.Blocks.NOT_GROW_LIGHTS)) {
            return NATURAL;
        }
        return state.hasProperty(BlockStateProperties.LIT) ? LAMP : AMBIENT;
    }

    /** What one block counts as once lit: {@link #of} with any {@code LIT} switched on. */
    public static GrowLight ofFixture(BlockState state) {
        return of(state.hasProperty(BlockStateProperties.LIT) ? state.setValue(BlockStateProperties.LIT, true) : state);
    }
}
