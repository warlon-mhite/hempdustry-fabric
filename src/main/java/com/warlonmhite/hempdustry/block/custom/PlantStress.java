package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

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
        return floor.isOf(Blocks.FARMLAND) && floor.get(FarmlandBlock.MOISTURE) == 0 ? DRY_FIELD : 0.0F;
    }

    /**
     * The record a plant carries after a growth step from {@code fromAge} to {@code toAge}: first the
     * light's verdict, then one roll at the worst odds among the ground and {@code extra} (the
     * overfeeding a bone meal step brings with it). {@code height} is how tall the plant stands grown,
     * which is where its light is read.
     */
    public static GrowLight afterStep(World world, BlockPos lower, GrowLight record, int fromAge, int toAge,
                                      int height, float extra, Random random) {
        GrowLight next = record.afterStep(fromAge, GrowLight.over(world, lower, height));
        if (next == GrowLight.STRESSED || toAge < GrowLight.FLOWERING_AGE) {
            return next;
        }
        float chance = Math.max(ofGround(world.getBlockState(lower.down())), extra);
        return chance > 0.0F && random.nextFloat() < chance ? GrowLight.STRESSED : next;
    }

    /** The odds a bone meal step on the plant at {@code lower} overfeeds it: flowering already, in a fed bed. */
    public static float ofBoneMeal(World world, BlockPos lower, BlockState plant) {
        BlockState floor = world.getBlockState(lower.down());
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
    public static void roll(World world, BlockPos lower, float chance, Random random) {
        BlockState plant = world.getBlockState(lower);
        if (!isFlowering(plant) || plant.get(GrowLight.PROPERTY) == GrowLight.STRESSED
                || plant.getBlock() instanceof CropBlock crop && crop.isMature(plant)
                || random.nextFloat() >= chance) {
            return;
        }
        Block block = plant.getBlock();
        for (BlockPos pos = lower; world.getBlockState(pos).isOf(block); pos = pos.up()) {
            world.setBlockState(pos, world.getBlockState(pos).with(GrowLight.PROPERTY, GrowLight.STRESSED),
                    Block.NOTIFY_LISTENERS);
        }
    }

    /**
     * The state a bee's growth step should write. A bee grows a crop with a raw
     * {@code setBlockState(withAge(age + 1))}, which skips the crop's own step; this gives that step
     * the same light reading and the same roll as any other, so a bee cannot carry a dry field
     * through its flowering untouched.
     */
    public static BlockState beeStep(World world, BlockPos pos, BlockState current, BlockState next,
                                     Random random) {
        if (!(current.getBlock() instanceof CropBlock crop) || !current.isOf(next.getBlock())
                || !next.contains(GrowLight.PROPERTY)) {
            return next;
        }
        int height = crop instanceof SativaCropBlock ? 3 : 2;
        return next.with(GrowLight.PROPERTY, afterStep(world, pos, current.get(GrowLight.PROPERTY),
                crop.getAge(current), crop.getAge(next), height, 0.0F, random));
    }

    /** A hemp plant at or past {@link GrowLight#FLOWERING_AGE}, with a record to lose. */
    public static boolean isFlowering(BlockState state) {
        return state.contains(GrowLight.PROPERTY) && state.contains(Properties.AGE_7)
                && state.get(Properties.AGE_7) >= GrowLight.FLOWERING_AGE;
    }
}
