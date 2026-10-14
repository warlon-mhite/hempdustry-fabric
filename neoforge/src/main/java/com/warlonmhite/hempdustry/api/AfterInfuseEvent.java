package com.warlonmhite.hempdustry.api;

import com.warlonmhite.hempdustry.item.custom.Quality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * Posted when a batch of cannabutter is collected from an Infuser, by any route -- a player, a hopper,
 * a pipe or the tub's own spout. The batch is closed out by then, so this is the only moment its
 * strength and quality are still knowable.
 */
public final class AfterInfuseEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final int strength;
    private final Quality quality;

    public AfterInfuseEvent(Level level, BlockPos pos, int strength, Quality quality) {
        this.level = level;
        this.pos = pos;
        this.strength = strength;
        this.quality = quality;
    }

    /** Where the Infuser stands. */
    public Level getLevel() {
        return level;
    }

    /** The Infuser's position. */
    public BlockPos getPos() {
        return pos;
    }

    /** How much hemp went into the batch, from 1 to the Infuser's batch cap. */
    public int getStrength() {
        return strength;
    }

    /** The grade the batch was collected at. */
    public Quality getQuality() {
        return quality;
    }
}
