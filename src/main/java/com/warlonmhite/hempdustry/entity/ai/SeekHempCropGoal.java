package com.warlonmhite.hempdustry.entity.ai;

import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

/**
 * Makes a creeper drift towards a hemp field.
 *
 * <h2>What this is for</h2>
 *
 * A creeper is green, and a mature hemp plant is two or three blocks of green. <b>A creeper standing
 * in a grown field is almost invisible</b>, and a player harvesting one is looking down at the crop
 * rather than out at the horizon. Pulling creepers towards the field turns a decorative farm into
 * something that has to be walled, lit and watched — which is the only real danger the mod's own
 * content has ever created, and it comes free out of a mob that was already in the game.
 *
 * <h2>Why it is a wander and not a hunt</h2>
 *
 * This sits at <b>priority 5</b>, level with {@code WanderAroundFarGoal} and below
 * {@code MeleeAttackGoal} at 4. A {@code GoalSelector} only lets a goal take a control away from a
 * <em>strictly</em> higher-priority one, so:
 *
 * <ul>
 *   <li>a creeper that has a target always attacks instead — the goal cannot make one ignore a
 *       player, which would be a bug rather than a feature;</li>
 *   <li>it competes with ordinary wandering rather than replacing it, so a field pulls creepers in
 *       over time instead of summoning every creeper in the chunk at once.</li>
 * </ul>
 *
 * <p>Priority 4 was rejected for the first reason: level with the attack goal, whichever started
 * first keeps the movement control, and a creeper already walking to a field would ignore the
 * player standing in it.
 *
 * <h2>Strain-agnostic, and it stays that way</h2>
 *
 * The target test is membership of {@code #hempdustry:hemp_crops}, so a third strain's crop is
 * attractive the day it joins the tag, and a datapack can add or remove plants without any code
 * knowing. It is deliberately not {@code #minecraft:crops}: creepers hunting wheat is a different
 * mod.
 */
public class SeekHempCropGoal extends MoveToTargetPosGoal {

    /**
     * How far a creeper notices a field, in blocks.
     *
     * <p>{@code MoveToTargetPosGoal} searches outward in rings and stops at the first hit, so the
     * usual cost is a handful of block reads rather than the whole cube; only a creeper standing in
     * an empty landscape pays for the full scan, and it pays it once every {@link #getInterval}.
     * Vanilla's own users of this goal run at 16 and 24.
     */
    private static final int RANGE = 16;
    /** Two blocks up or down. A field on the next terrace counts; one at the bottom of a ravine does not. */
    private static final int MAX_Y_DIFFERENCE = 2;
    /** Slightly above {@code WanderAroundFarGoal}'s 0.8, so a creeper that commits actually arrives. */
    private static final double SPEED = 0.9;
    /**
     * Ticks between searches when the last one found nothing — three seconds, against the base
     * class's 200–400. The default cadence is tuned for a mob that stays near one place; a creeper
     * is wandering the whole time, so a slow retry means it walks past a field without ever looking.
     */
    private static final int SEARCH_INTERVAL = 60;

    public SeekHempCropGoal(PathAwareEntity creeper) {
        super(creeper, SPEED, RANGE, MAX_Y_DIFFERENCE);
    }

    /**
     * The config's off switch lives here rather than at the mixin, so
     * {@code /hempdustry reload} takes effect on creepers that already exist. Goals are built in
     * the entity's constructor; a check at the mixin would freeze the setting at spawn time and
     * every creeper already loaded would keep the old behaviour for ever.
     */
    @Override
    public boolean canStart() {
        return HempdustryConfig.get().world().creepersSeekHemp() && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return HempdustryConfig.get().world().creepersSeekHemp() && super.shouldContinue();
    }

    @Override
    protected int getInterval(PathAwareEntity mob) {
        return SEARCH_INTERVAL;
    }

    @Override
    protected boolean isTargetPos(WorldView world, BlockPos pos) {
        return world.getBlockState(pos).isIn(ModTags.Blocks.HEMP_CROPS);
    }
}
