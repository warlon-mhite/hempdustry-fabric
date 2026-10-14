package com.warlonmhite.hempdustry.api;

import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * Posted after a hit has landed -- effects applied (or a green-out rolled instead), sound played,
 * cooldown not yet started. For an addon's own "stoned" effect, a tolerance or addiction system, or a
 * quest task like "smoke a Lemon Haze spliff" that would otherwise have to scrape advancements.
 *
 * <p>{@link #getDevice} is the stack that was smoked, still packed at this point -- read
 * {@link #getContents} rather than the stack, because a spliff's is about to be gone.
 */
public final class AfterSmokeEvent extends Event {
    private final Player player;
    private final SmokeContents contents;
    private final ItemStack device;

    public AfterSmokeEvent(Player player, SmokeContents contents, ItemStack device) {
        this.player = player;
        this.contents = contents;
        this.device = device;
    }

    /** Who took the hit. */
    public Player getPlayer() {
        return player;
    }

    /** What was smoked. */
    public SmokeContents getContents() {
        return contents;
    }

    /** The stack that was smoked. */
    public ItemStack getDevice() {
        return device;
    }
}
