package com.warlonmhite.hempdustry.api;

import com.warlonmhite.hempdustry.item.custom.Quality;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * Posted when a player eats an edible -- a brownie, a space cookie, a dawamesk, cannabutter toast,
 * bhang, or a slice of Space Cake. The Hemp Seed Bar is plain food and never posts it.
 *
 * <p><b>Smoking and eating are two separate doors into the same room, and an addon that watches only
 * {@link AfterSmokeEvent} sees half the traffic.</b> A tolerance or addiction system built on the hit
 * alone would count every joint, miss every brownie, and read a player as sober while they are four
 * tiers into a cake.
 *
 * <p>It is posted at the <b>moment of eating</b>, not at onset. An edible's effects are queued behind
 * a come-up of up to 4:20 and then ramp in stages, so there is no single later instant that means
 * "this happened"; the swallow is the event. A listener wanting to act when the high actually lands
 * should schedule its own delay.
 */
public final class AfterEatEvent extends Event {
    private final Player player;
    private final int tier;
    private final Quality quality;

    public AfterEatEvent(Player player, int tier, Quality quality) {
        this.player = player;
        this.tier = tier;
        this.quality = quality;
    }

    /** Who ate it. */
    public Player getPlayer() {
        return player;
    }

    /**
     * The potency, 1..4 -- the same dial the tooltip shows, already resolved from the cannabutter that
     * went in. Nothing is posted for a tier-0 edible, which is one nobody dosed.
     */
    public int getTier() {
        return tier;
    }

    /** The grade of the cannabutter it was baked with. */
    public Quality getQuality() {
        return quality;
    }
}
