package com.warlonmhite.hempdustry.platform;

import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * What the mod keeps on a player, saved with them: the edibles they have eaten that have not landed
 * yet. Kept through the End portal's return home; dropped on death.
 */
public interface PlayerData {

    /** The player's pending edibles, oldest first; empty when there are none. */
    List<EdibleScheduler.Pending> pendingEdibles(Player player);

    /** Replaces the player's pending edibles; an empty list removes them. */
    void setPendingEdibles(Player player, List<EdibleScheduler.Pending> pending);
}
