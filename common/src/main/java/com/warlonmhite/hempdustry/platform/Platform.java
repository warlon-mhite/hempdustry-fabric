package com.warlonmhite.hempdustry.platform;

import net.minecraft.core.BlockPos;
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
     * The growth odds the loader's own crops read at {@code pos} for {@code crop}: vanilla's farmland
     * count and crowding halving, which NeoForge reworks to let a modded soil count as farmland. So a
     * hemp crop grows on each loader exactly as wheat does there.
     */
    float cropGrowthSpeed(BlockState crop, BlockGetter level, BlockPos pos);
}
