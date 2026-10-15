package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.api.HempdustryEvents;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.platform.AddonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The addon events as {@link HempdustryEvents}, Fabric's {@code Event}s, frozen since 2.0. */
public final class FabricAddonEvents implements AddonEvents {
    @Override
    public boolean allowSmoke(Player player, SmokeContents contents, ItemStack device) {
        return HempdustryEvents.ALLOW_SMOKE.invoker().allowSmoke(player, contents, device);
    }

    @Override
    public void afterSmoke(Player player, SmokeContents contents, ItemStack device) {
        HempdustryEvents.AFTER_SMOKE.invoker().afterSmoke(player, contents, device);
    }

    @Override
    public void afterEat(Player player, int tier, Quality quality) {
        HempdustryEvents.AFTER_EAT.invoker().onEaten(player, tier, quality);
    }

    @Override
    public void afterInfuse(Level level, BlockPos pos, int strength, Quality quality) {
        HempdustryEvents.AFTER_INFUSE.invoker().onInfused(level, pos, strength, quality);
    }
}
