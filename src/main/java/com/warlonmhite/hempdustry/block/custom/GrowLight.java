package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * The light a hemp plant grew under — a <b>record the plant keeps</b>, never a reading of the lamp
 * at harvest. Canonical on the LOWER segment, like the age and the trim flags.
 *
 * <p>The first growth step writes the tier overhead. Every later step, natural or bone meal, can
 * only <b>lower</b> it to the weakest light the plant has grown under; losing the light entirely
 * turns a flowering plant {@link #STRESSED} (a light-interrupted plant going hermaphrodite and
 * seeding its own buds) and a younger one back to {@link #NATURAL}. So the harvest cannot be
 * cheated by moving a lamp over at the end, one lamp cannot be shuttled between plants (growth
 * steps land at random moments), and swapping a Grow Lamp for glowstone halfway through harvests as
 * glowstone. Bees are the one step that does not look — their write goes straight through
 * {@code BeeGrowCropsGoalMixin}, which only carries the record across.
 *
 * <p>The constants are in tier order, weakest first, and {@link #afterStep} relies on it.
 */
public enum GrowLight implements StringIdentifiable {
    /** Sun, torches, or nothing counted — the baseline. */
    NATURAL("natural"),
    /** Any other block at light 15: glowstone, sea lantern, shroomlight, froglights, lanterns. */
    AMBIENT("ambient"),
    /** A block at light 15 that is switched on: a redstone lamp, a copper bulb, most modded lamps. */
    LAMP("lamp"),
    /** {@code #hempdustry:grow_lamps} — the Grow Lamp, and whatever a pack adds. */
    GROW_LAMP("grow_lamp"),
    /** Lost its lamp while flowering. Harvests a bud short and seeded. */
    STRESSED("stressed");

    public static final EnumProperty<GrowLight> PROPERTY = EnumProperty.of("light", GrowLight.class);

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
    public String asString() {
        return this.name;
    }

    /** Any light the plant grew under that was not the sun. Stress only ever comes from losing one. */
    public boolean artificial() {
        return this != NATURAL;
    }

    /** The record after one growth step taken from {@code fromAge}, with {@code now} overhead. */
    public GrowLight afterStep(int fromAge, GrowLight now) {
        if (fromAge == 0) {
            return now;
        }
        if (this == NATURAL || this == STRESSED) {
            return this;
        }
        if (now == NATURAL) {
            return fromAge + 1 >= FLOWERING_AGE ? STRESSED : NATURAL;
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
    public static GrowLight over(BlockView world, BlockPos lower, int height) {
        GrowLight best = NATURAL;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                GrowLight found = scan(world, lower.add(dx, height, dz));
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
    private static GrowLight scan(BlockView world, BlockPos from) {
        for (int i = 0; i < REACH; i++) {
            BlockState state = world.getBlockState(from.up(i));
            GrowLight tier = of(state);
            if (tier != NATURAL || state.isOpaqueFullCube()) {
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
        if (state.isIn(ModTags.Blocks.GROW_LAMPS)) {
            return !state.contains(Properties.LIT) || state.get(Properties.LIT) ? GROW_LAMP : NATURAL;
        }
        if (state.getLuminance() < 15 || state.isIn(ModTags.Blocks.NOT_GROW_LIGHTS)) {
            return NATURAL;
        }
        return state.contains(Properties.LIT) ? LAMP : AMBIENT;
    }
}
