package com.warlonmhite.hempdustry.neoforge;

import com.warlonmhite.hempdustry.api.AfterEatEvent;
import com.warlonmhite.hempdustry.api.AfterInfuseEvent;
import com.warlonmhite.hempdustry.api.AfterSmokeEvent;
import com.warlonmhite.hempdustry.api.AllowSmokeEvent;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.platform.AddonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

/** The addon events on NeoForge: the four classes of {@code com.warlonmhite.hempdustry.api}, on NeoForge's game bus. */
public final class NeoForgeAddonEvents implements AddonEvents {
    @Override
    public boolean allowSmoke(Player player, SmokeContents contents, ItemStack device) {
        return !NeoForge.EVENT_BUS.post(new AllowSmokeEvent(player, contents, device)).isCanceled();
    }

    @Override
    public void afterSmoke(Player player, SmokeContents contents, ItemStack device) {
        NeoForge.EVENT_BUS.post(new AfterSmokeEvent(player, contents, device));
    }

    @Override
    public void afterEat(Player player, int tier, Quality quality) {
        NeoForge.EVENT_BUS.post(new AfterEatEvent(player, tier, quality));
    }

    @Override
    public void afterInfuse(Level level, BlockPos pos, int strength, Quality quality) {
        NeoForge.EVENT_BUS.post(new AfterInfuseEvent(level, pos, strength, quality));
    }
}
