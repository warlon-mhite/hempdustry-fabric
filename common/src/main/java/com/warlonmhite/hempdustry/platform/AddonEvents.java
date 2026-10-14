package com.warlonmhite.hempdustry.platform;

import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The events other mods listen to, fired on whichever loader's event system they live on. The mod
 * fires them here; each loader's implementation hands them to its own listeners.
 */
public interface AddonEvents {

    /** Whether {@code player} may take a hit; any listener may refuse. */
    boolean allowSmoke(Player player, SmokeContents contents, ItemStack device);

    /** After a hit was taken. */
    void afterSmoke(Player player, SmokeContents contents, ItemStack device);

    /** After an edible of {@code tier} and {@code quality} was eaten. */
    void afterEat(Player player, int tier, Quality quality);

    /** After a batch of cannabutter was taken out of the Infuser at {@code pos}. */
    void afterInfuse(Level level, BlockPos pos, int strength, Quality quality);
}
