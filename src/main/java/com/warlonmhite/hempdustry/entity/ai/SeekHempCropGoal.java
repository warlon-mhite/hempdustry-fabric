package com.warlonmhite.hempdustry.entity.ai;

import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Lets a creeper's ordinary wandering end up in a hemp field now and then.
 *
 * <h2>What this is for</h2>
 *
 * A creeper is green, and a grown hemp plant is two or three blocks of green. <b>A creeper standing
 * in a grown field is almost invisible</b>, and a player harvesting one is looking down at the crop
 * rather than out at the horizon. An open field should be a place you glance around before walking
 * into — never a place creepers visibly queue up at.
 *
 * <h2>It is a wander, and nothing else</h2>
 *
 * The first version was a {@code MoveToTargetPosGoal}, which walks to the nearest target and then
 * <em>holds on for up to a minute</em>; its next search finds the plant it is standing beside, so a
 * creeper that reached a field stayed there for good, and every creeper within 16 blocks of one
 * plant came over. That is a beacon, and a beacon is a mob farm.
 *
 * <p>This is a {@link WanderAroundGoal} instead: the same speed, the same per-tick odds of starting,
 * the same stop-on-arrival and the same "nobody is near, so stand still" rule as the wandering it
 * sits beside at priority 5. The only thing that differs is <em>where</em> it wanders to, so there is
 * no moment a player could point at. Priority 5 also keeps vanilla's rules: a creeper with a target
 * always attacks, because the attack goal at 4 takes the movement control from anything at 5.
 *
 * <h2>How a field is noticed</h2>
 *
 * A creeper does not scan for the nearest plant. It picks a few random columns around itself and
 * looks at what stands in them, so <b>the odds of noticing a field grow with the field's area</b>:
 * a 9×9 field is found in a minute or so, a lone plant almost never, and nothing has to count plants.
 * A column only counts if:
 *
 * <ul>
 *   <li><b>the plant is at least two blocks tall</b> — tall enough to hide a creeper. A freshly sown
 *       field draws nothing, so the danger arrives as the crop comes in, which a player can see
 *       happening;</li>
 *   <li><b>the plant is the top of its column</b> ({@code WORLD_SURFACE}) — open sky. A roof, glass
 *       included, hides a field, so anything grown indoors or in a dark mob-farm room draws
 *       nothing;</li>
 *   <li><b>no other creeper is already in it</b> ({@link #TAKEN_RADIUS}). A field draws one creeper
 *       at a time, which is the whole anti-farm ceiling: throughput can never exceed "one creeper
 *       wandered in", however much hemp is planted round a kill chamber.</li>
 * </ul>
 *
 * <h2>What a creeper does once it is there</h2>
 *
 * It prowls: a few short wanders from one plant to another in the same field
 * ({@link #MIN_PROWLS}–{@link #MAX_PROWLS}), each still at vanilla's pace, and after each one it
 * <b>lurks</b> — stands in the crop for two to six seconds. A creeper standing still among the
 * plants is the moment a player walks into, and it is the part vanilla wandering cannot supply:
 * between two ordinary wanders a creeper is idle, and vanilla's own wander would take it out of the
 * field a third of the time. Holding the movement control through the pause is what keeps a visit
 * a visit. Then it loses interest for a minute or three ({@link #MIN_REST}–{@link #MAX_REST}) and
 * is an ordinary creeper again.
 *
 * <p><b>Measured</b>, in a pen with ripe fields and seedling fields of equal area: the old goal kept
 * 89% of all creeper time inside hemp, 94% of it standing still, seedlings and a lone plant
 * included. Without the lurk this goal was indistinguishable from vanilla. With it, four creepers
 * over two game hours spent about four times vanilla's share of their time in a grown field, never
 * more than two at once and moving about as much as any wandering creeper.
 *
 * <p>And <b>only about half of all creepers care at all</b>, decided by the creeper's UUID so it holds
 * across saves without storing anything. The rest ignore hemp entirely, which is most of why the
 * behaviour reads as chance rather than as a rule.
 *
 * <p>Nothing about the goal is saved: prowls and rest reset when a chunk reloads, which only ever
 * makes a creeper look at the fields around it once more.
 *
 * <h2>Strain-agnostic, and it stays that way</h2>
 *
 * The plant test is membership of {@code #hempdustry:hemp_crops}, so a third strain's crop counts
 * the day it joins the tag, and a datapack can add or remove plants without any code knowing. It is
 * deliberately not {@code #minecraft:crops}: creepers hunting wheat is a different mod.
 */
public class SeekHempCropGoal extends WanderAroundGoal {

    /** {@code WanderAroundFarGoal}'s speed on a creeper. Walking faster towards a field would give it away. */
    private static final double SPEED = 0.8;
    /** One in this many ticks a creeper glances around for a field — half as often as it wanders. */
    private static final int NOTICE_CHANCE = 240;
    /** Half the side of the square a creeper looks across, in blocks. */
    private static final int NOTICE_RANGE = 24;
    /** Columns looked at per glance. A 9×9 field 15 blocks off is hit about one glance in four. */
    private static final int NOTICE_SAMPLES = 8;
    /** Odds of the next prowl step once a lurk is over: six times vanilla's, so the field usually wins. */
    private static final int PROWL_CHANCE = 20;
    private static final int PROWL_RANGE = 5;
    private static final int PROWL_SAMPLES = 6;
    private static final int MIN_PROWLS = 3;
    private static final int MAX_PROWLS = 6;
    /** Two to six seconds standing in the crop after each step. */
    private static final int MIN_LURK = 40;
    private static final int MAX_LURK = 120;
    /** One to three minutes before the same creeper looks at hemp again. */
    private static final int MIN_REST = 1200;
    private static final int MAX_REST = 3600;
    /** A creeper this close to a column is already in that field. */
    private static final double TAKEN_RADIUS = 8.0;
    /** A plant's roots this far above or below the creeper's feet are out of reach. */
    private static final int MAX_Y_DIFFERENCE = 3;

    private int prowlsLeft;
    private long restingUntil;
    /** When the current lurk ends; 0 while still walking. */
    private long lurkingUntil;

    public SeekHempCropGoal(PathAwareEntity creeper) {
        super(creeper, SPEED, NOTICE_CHANCE);
    }

    /**
     * Whether this creeper is one of the half that notice hemp at all. Read off the UUID, which
     * already survives saving, so the answer never changes for a given creeper.
     */
    public static boolean caresAboutHemp(Entity creeper) {
        return (creeper.getUuid().getLeastSignificantBits() & 1L) == 0L;
    }

    /**
     * The config's off switch lives here rather than at the mixin, so
     * {@code /hempdustry reload} takes effect on creepers that already exist. Goals are built in
     * the entity's constructor; a check at the mixin would freeze the setting at spawn time.
     */
    @Override
    public boolean canStart() {
        if (!HempdustryConfig.get().world().creepersSeekHemp() || !caresAboutHemp(mob)
                || mob.getEntityWorld().getTime() < restingUntil) {
            return false;
        }
        setChance(prowlsLeft > 0 ? PROWL_CHANCE : NOTICE_CHANCE);
        return super.canStart();
    }

    /**
     * Walking, then lurking where the walk ended. A creeper with a target never gets here: the
     * attack goal at priority 4 takes the movement control from this one, lurk or no lurk.
     */
    @Override
    public boolean shouldContinue() {
        if (!HempdustryConfig.get().world().creepersSeekHemp() || mob.hasControllingPassenger()) {
            return false;
        }
        if (!mob.getNavigation().isIdle()) {
            return true;
        }
        long now = mob.getEntityWorld().getTime();
        if (lurkingUntil == 0L) {
            lurkingUntil = now + mob.getRandom().nextBetween(MIN_LURK, MAX_LURK);
        }
        return now < lurkingUntil;
    }

    @Override
    public void stop() {
        super.stop();
        lurkingUntil = 0L;
    }

    @Override
    protected @Nullable Vec3d getWanderTarget() {
        boolean prowling = prowlsLeft > 0;
        BlockPos plant = prowling
                ? findCover(mob, PROWL_RANGE, PROWL_SAMPLES)
                : findCover(mob, NOTICE_RANGE, NOTICE_SAMPLES);
        if (plant == null) {
            // A prowl that finds no more field has walked out of it, or been led out; either way
            // the visit is over.
            if (prowling) {
                rest();
            }
            return null;
        }
        return Vec3d.ofBottomCenter(plant);
    }

    @Override
    public void start() {
        super.start();
        Random random = mob.getRandom();
        if (prowlsLeft == 0) {
            prowlsLeft = random.nextBetween(MIN_PROWLS, MAX_PROWLS);
        } else if (--prowlsLeft == 0) {
            rest();
        }
    }

    private void rest() {
        prowlsLeft = 0;
        restingUntil = mob.getEntityWorld().getTime() + mob.getRandom().nextBetween(MIN_REST, MAX_REST);
    }

    /**
     * Looks at {@code samples} random columns within {@code range} of the creeper and returns the
     * root of the first grown, open-sky hemp plant no other creeper is standing by, or null.
     */
    public static @Nullable BlockPos findCover(PathAwareEntity creeper, int range, int samples) {
        World world = creeper.getEntityWorld();
        Random random = creeper.getRandom();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int i = 0; i < samples; i++) {
            int x = creeper.getBlockX() + random.nextBetween(-range, range);
            int z = creeper.getBlockZ() + random.nextBetween(-range, range);
            // Never load a chunk to answer a wandering mob.
            if (!world.isPosLoaded(pos.set(x, creeper.getBlockY(), z))) {
                continue;
            }
            pos.setY(world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1);
            int height = 0;
            while (world.getBlockState(pos).isIn(ModTags.Blocks.HEMP_CROPS)) {
                pos.move(0, -1, 0);
                height++;
            }
            if (height < 2) {
                continue;
            }
            BlockPos root = pos.up();
            if (Math.abs(root.getY() - creeper.getBlockY()) > MAX_Y_DIFFERENCE
                    || isTaken(world, root, creeper)) {
                continue;
            }
            return root;
        }
        return null;
    }

    private static boolean isTaken(World world, BlockPos root, Entity self) {
        return !world.getEntitiesByClass(CreeperEntity.class, new Box(root).expand(TAKEN_RADIUS),
                other -> other != self).isEmpty();
    }
}
