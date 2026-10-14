package com.warlonmhite.hempdustry.api;

import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Vetoes a hit before anything happens -- no effects, no cooldown, nothing consumed. For a sober
 * zone, a jail plugin, or an accessibility mod suppressing the whole mechanic.
 *
 * <p>Cancel it to refuse the hit; the item simply passes, exactly as it does when it is empty or
 * still cooling down. The check is server-side, so a client may swing its arm before the server
 * declines -- the same thing vanilla does with any refused use.
 */
public final class AllowSmokeEvent extends Event implements ICancellableEvent {
    private final Player player;
    private final SmokeContents contents;
    private final ItemStack device;

    public AllowSmokeEvent(Player player, SmokeContents contents, ItemStack device) {
        this.player = player;
        this.contents = contents;
        this.device = device;
    }

    /** Who is about to take the hit. */
    public Player getPlayer() {
        return player;
    }

    /** What is in the bowl, the spliff or the vaporizer. */
    public SmokeContents getContents() {
        return contents;
    }

    /** The stack being smoked. */
    public ItemStack getDevice() {
        return device;
    }
}
