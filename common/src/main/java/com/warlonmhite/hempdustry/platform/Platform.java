package com.warlonmhite.hempdustry.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

/** Facts about the running game that only the loader knows. */
public interface Platform {

    /** The folder every mod's config file goes in. */
    Path configDir();

    /** This mod's version, as the loader read it from the mod's manifest. */
    String modVersion();

    /**
     * The loader's name as Modrinth files a version under it ({@code fabric}, {@code neoforge}): the
     * update checker asks for the builds made for the loader it runs on.
     */
    String loaderName();

    /**
     * The growth odds the loader's own crops read at {@code pos} for {@code crop}: vanilla's farmland
     * count and crowding halving, which NeoForge reworks to let a modded soil count as farmland. So a
     * hemp crop grows on each loader exactly as wheat does there.
     */
    float cropGrowthSpeed(BlockState crop, BlockGetter level, BlockPos pos);

    /**
     * Whether {@code enderman} would not see {@code player} meet its eyes for what the player wears:
     * vanilla's carved pumpkin, and on NeoForge also a modded mask and the anger event an addon may
     * cancel. Asked of a player who breaks Ender Haze, which provokes only the endermen this is false for.
     */
    boolean disguisedFrom(Player player, Enderman enderman);
}
