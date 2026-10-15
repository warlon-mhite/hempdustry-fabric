package com.warlonmhite.hempdustry.neoforge;

import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.platform.AddonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The addon events on NeoForge: none yet. Fabric's four ({@code HempdustryEvents}) get twins on
 * NeoForge's own bus, carrying the same payloads; until those are named, nothing listens, so every
 * hit is allowed and nothing is told.
 */
public final class NeoForgeAddonEvents implements AddonEvents {
    @Override
    public boolean allowSmoke(Player player, SmokeContents contents, ItemStack device) {
        return true;
    }

    @Override
    public void afterSmoke(Player player, SmokeContents contents, ItemStack device) {
    }

    @Override
    public void afterEat(Player player, int tier, Quality quality) {
    }

    @Override
    public void afterInfuse(Level level, BlockPos pos, int strength, Quality quality) {
    }
}
