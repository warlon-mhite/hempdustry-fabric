package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Everything besides a lost light that can turn a flowering plant {@link GrowLight#STRESSED}.
 *
 * <p>Losing the light is certain; everything here is a roll. The odds sit on the equipment, where a
 * player can see the cause: dry soil shows as vanilla's dry farmland, a dry tray as an empty sight
 * tube, a dead pump as a dark indicator with no bubbles, a fed bed as moist soil with compost on it
 * or a green tube. <b>Beds carry the risk and fields barely do</b> — stress is what indoor growing
 * pays for its extra buds, and a field only feels it when it is left bone dry.
 *
 * <table>
 *   <tr><th>cause</th><th>where</th><th>odds</th></tr>
 *   <tr><td>bone meal on a flowering plant in a fed bed</td><td>pot, tray</td><td>{@link #OVERFED} per bone meal</td></tr>
 *   <tr><td>a bottle poured into a wet pot</td><td>pot</td><td>{@link #OVERWATERED} per bottle</td></tr>
 *   <tr><td>a growth step in a dry bed</td><td>pot, tray</td><td>{@link #DRY_BED} per step</td></tr>
 *   <tr><td>a growth step in a tray with water and the pump off</td><td>tray</td><td>{@link #STAGNANT} per step</td></tr>
 *   <tr><td>a growth step on farmland at moisture 0</td><td>field</td><td>{@link #DRY_FIELD} per step</td></tr>
 * </table>
 *
 * <p>Only a flowering plant is at risk, as with the light: a young one shrugs it off. When two causes
 * meet on one step the plant rolls <b>once</b>, at the worse odds. Stress is kept for good, exactly
 * as the light's is — a plant that has gone hermaphrodite does not go back.
 */
public final class PlantStress {
    /**
     * Our bone meal is one stage a use and a third of uses fail, so a plant rushed from flowering to
     * ripe takes three of these rolls: about 30% at 1 in 9. At 1 in 4 it was 58%, which is harsh.
     */
    public static final float OVERFED = 1.0F / 9;
    public static final float OVERWATERED = 1.0F / 4;
    public static final float DRY_BED = 1.0F / 3;
    /** Still water and no air: the roots rot, which is the classic way a deep-water-culture grow fails. */
    public static final float STAGNANT = 1.0F / 6;
    public static final float DRY_FIELD = 1.0F / 20;
    /**
     * The age from which a lost light, or a light leaking into the night, stresses a plant touchy about
     * its light: a stage before {@link GrowLight#FLOWERING_AGE}, as a weak plant would be.
     */
    public static final int TOUCHY_AGE = 3;

    private PlantStress() {
    }

    /**
     * The odds that one growth step stresses a flowering plant standing on {@code floor}. A bed of ours
     * answers for itself; farmland only once it is bone dry, which vanilla's {@code FarmlandBlock} only
     * lets happen after about seven random ticks with no water in reach and no rain.
     */
    public static float ofGround(BlockState floor) {
        if (floor.getBlock() instanceof GrowMedium medium) {
            return medium.stress(floor);
        }
        return floor.is(Blocks.FARMLAND) && floor.getValue(FarmlandBlock.MOISTURE) == 0 ? DRY_FIELD : 0.0F;
    }

    /**
     * The record a plant carries after a growth step from {@code fromAge} to {@code toAge}: first the
     * light's verdict, then one roll at the worst odds among the ground and {@code extra} (the
     * overfeeding a bone meal step brings with it). {@code height} is how tall the plant stands grown,
     * which is where its light is read. A plant that is not {@code stressable} is never stressed:
     * losing its light only lowers its record to the sun's, as it does for a young plant, and nothing
     * is rolled.
     */
    public static GrowLight afterStep(Level world, BlockPos lower, GrowLight record, int fromAge, int toAge,
                                      int height, float extra, RandomSource random, boolean stressable) {
        return afterStep(world, lower, record, fromAge, toAge, height, extra, random, stressable, false);
    }

    /**
     * As above; a {@code touchy} plant reads its light by {@link #touchyStep}: stressed a stage early
     * by a lost light, and by a light that leaks into its night.
     */
    public static GrowLight afterStep(Level world, BlockPos lower, GrowLight record, int fromAge, int toAge,
                                      int height, float extra, RandomSource random, boolean stressable,
                                      boolean touchy) {
        // The open-sky record rides the same step as the light record, on every growth route.
        PlantRecordBlockEntity.recordSky(world, lower, fromAge, height);
        GrowLight next = touchy ? touchyStep(world, lower, record, fromAge, toAge, height)
                : record.afterStep(fromAge, GrowLight.over(world, lower, height));
        if (!stressable) {
            return next == GrowLight.STRESSED ? GrowLight.NATURAL : next;
        }
        if (next == GrowLight.STRESSED || toAge < GrowLight.FLOWERING_AGE) {
            return next;
        }
        float chance = Math.max(ofGround(world.getBlockState(lower.below())), extra);
        return chance > 0.0F && random.nextFloat() < chance ? GrowLight.STRESSED : next;
    }

    /**
     * The light record after one growth step of a plant touchy about its light (White Widow), which a
     * light at night stresses: the indoor grower's light leak, a lit bulb in the dark period turning a
     * flowering plant hermaphrodite.
     *
     * <ul>
     *   <li><b>Day and night</b> are what a daylight detector beside the plant would say ({@link #hours}),
     *       so a Grow Lamp wired to one is a 12/12 timer: lit whenever the plant reads day, dark whenever
     *       it reads night, in any weather. Around each switch lies a twilight in which neither a leak nor
     *       a loss counts, as long as the detector takes to notice and the lamp to go out.</li>
     *   <li><b>A leak</b>, at night and from {@link #TOUCHY_AGE}, is a lit light over the plant (anything
     *       the record counts) or a light-15 block touching it ({@link #leaks}). It stresses the plant.</li>
     *   <li><b>Darkness at night never counts as a lost light.</b> A dark night keeps the record as it
     *       was; a dark lamp still hanging there counts as the lamp it is, which is also what the plant's
     *       first step reads if it falls at night.</li>
     *   <li><b>By day</b>, the record's own ladder, but a lost light stresses from {@link #TOUCHY_AGE}.</li>
     * </ul>
     */
    static GrowLight touchyStep(Level world, BlockPos lower, GrowLight record, int fromAge, int toAge, int height) {
        Hours hours = hours(world, lower);
        if (hours == Hours.DAY) {
            return record.afterStep(fromAge, GrowLight.over(world, lower, height), TOUCHY_AGE);
        }
        if (hours == Hours.NIGHT && toAge >= TOUCHY_AGE && leaks(world, lower, height)) {
            return GrowLight.STRESSED;
        }
        GrowLight fixture = GrowLight.fixtureOver(world, lower, height);
        return fixture == GrowLight.NATURAL ? record : record.afterStep(fromAge, fixture, TOUCHY_AGE);
    }

    /** Day, night, or the twilight around the switch between them, as a plant touchy about its light keeps them. */
    public enum Hours { DAY, TWILIGHT, NIGHT }

    /**
     * How far from a daylight detector's switch, in its own signal before rounding, a step must fall to
     * count as day or night. The detector looks at the sky once a second and a lamp goes out four ticks
     * after it, so a lamp may lag the sky by 24 ticks; a quarter of a signal step is at least 50 ticks of
     * a clear dusk or dawn, and longer under rain, where the signal moves slower.
     */
    static final float TWILIGHT_SIGNAL = 0.25F;

    /**
     * The hours at {@code pos}, read the way a daylight detector with open sky above it reads them: night
     * where it would give no signal, day where it would give one. A dimension with no sky light has no
     * night at all, as the eyeblossom's attribute has none there.
     */
    public static Hours hours(Level world, BlockPos pos) {
        if (!world.dimensionType().hasSkyLight()) {
            return Hours.DAY;
        }
        float signal = daylightSignal(world, pos);
        if (signal < 0.5F - TWILIGHT_SIGNAL) {
            return Hours.NIGHT;
        }
        return signal >= 0.5F + TWILIGHT_SIGNAL ? Hours.DAY : Hours.TWILIGHT;
    }

    /**
     * What a daylight detector at {@code pos} under open sky would output, before it rounds: vanilla's
     * {@code DaylightDetectorBlock#updateSignalStrength} with sky light 15 — the sky's brightness less
     * the darkening of night and weather, times the cosine of the sun's angle eased a fifth toward noon.
     * It rounds to 0 below one half.
     */
    static float daylightSignal(Level world, BlockPos pos) {
        int sky = Math.max(15 - world.getSkyDarken(), 0);
        if (sky == 0) {
            return 0.0F;
        }
        float angle = world.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, pos) * Mth.DEG_TO_RAD;
        float noon = angle < Mth.PI ? 0.0F : Mth.TWO_PI;
        angle += (noon - angle) * 0.2F;
        return sky * Mth.cos(angle);
    }

    /**
     * Whether light leaks onto the plant whose LOWER is at {@code lower}: a lit light over it, as the
     * record reads one, or a light-15 block touching one of its segments on any face — a lantern, a
     * lit lamp, a lit campfire, fire or lava beside it. Only 15: a torch (14) beside a field never
     * counts, nor anything a block away, so a torch-lit farm is safe.
     */
    public static boolean leaks(BlockGetter world, BlockPos lower, int height) {
        if (GrowLight.over(world, lower, height) != GrowLight.NATURAL) {
            return true;
        }
        Block plant = world.getBlockState(lower).getBlock();
        for (int i = 0; i < height; i++) {
            BlockPos segment = lower.above(i);
            if (i > 0 && !world.getBlockState(segment).is(plant)) {
                break;
            }
            for (Direction side : Direction.values()) {
                BlockState touching = world.getBlockState(segment.relative(side));
                if (!touching.is(plant) && touching.getLightEmission() >= 15) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The odds a bone meal step on the plant at {@code lower} overfeeds it: flowering already, in a fed bed. */
    public static float ofBoneMeal(Level world, BlockPos lower, BlockState plant) {
        BlockState floor = world.getBlockState(lower.below());
        return isFlowering(plant) && floor.getBlock() instanceof GrowMedium medium && medium.fed(floor)
                ? OVERFED : 0.0F;
    }

    /**
     * Rolls {@code chance} against the plant whose LOWER is at {@code lower}, outside any growth step,
     * and on a hit writes the record up every segment at once, so the plant shows it straight away.
     * A ripe plant is past it: stress is a growth failure, a ripe plant grows no more, and a real
     * plant cut at maturity has no weeks left to go seedy. Vanilla never costs a finished crop its
     * yield either.
     */
    public static void roll(Level world, BlockPos lower, float chance, RandomSource random) {
        BlockState plant = world.getBlockState(lower);
        if (!isFlowering(plant) || plant.getValue(GrowLight.PROPERTY) == GrowLight.STRESSED
                || plant.getBlock() instanceof CropBlock crop && crop.isMaxAge(plant)
                || random.nextFloat() >= chance) {
            return;
        }
        Block block = plant.getBlock();
        for (BlockPos pos = lower; world.getBlockState(pos).is(block); pos = pos.above()) {
            world.setBlock(pos, world.getBlockState(pos).setValue(GrowLight.PROPERTY, GrowLight.STRESSED),
                    Block.UPDATE_CLIENTS);
        }
    }

    /**
     * The state a bee's growth step should write. A bee grows a crop with a raw
     * {@code setBlockAndUpdate(getStateForAge(age + 1))}, which skips the crop's own step; this gives that step
     * the same light reading and the same roll as any other, so a bee cannot carry a dry field
     * through its flowering untouched.
     */
    public static BlockState beeStep(Level world, BlockPos pos, BlockState current, BlockState next,
                                     RandomSource random) {
        if (!(current.getBlock() instanceof CropBlock crop) || !current.is(next.getBlock())
                || !next.hasProperty(GrowLight.PROPERTY)
                || crop instanceof IndicaCropBlock indica && !indica.recordsLight()) {
            return next;
        }
        int height = crop instanceof EnderHazeCropBlock ? EnderHazeCropBlock.HEIGHT : crop instanceof SativaCropBlock ? 3 : 2;
        boolean stressable = !(crop instanceof SativaCropBlock sativa) || sativa.canBeStressed();
        boolean touchy = crop instanceof IndicaCropBlock indica && indica.leaksAtNight();
        return next.setValue(GrowLight.PROPERTY, afterStep(world, pos, current.getValue(GrowLight.PROPERTY),
                crop.getAge(current), crop.getAge(next), height, 0.0F, random, stressable, touchy));
    }

    /** A hemp plant at or past {@link GrowLight#FLOWERING_AGE}, with a record to lose. */
    public static boolean isFlowering(BlockState state) {
        return state.hasProperty(GrowLight.PROPERTY) && state.hasProperty(BlockStateProperties.AGE_7)
                && state.getValue(BlockStateProperties.AGE_7) >= GrowLight.FLOWERING_AGE;
    }
}
