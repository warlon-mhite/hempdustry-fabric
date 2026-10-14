package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

public final class FabricPlatform implements Platform {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Hempdustry.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    @Override
    public float cropGrowthSpeed(BlockState crop, BlockGetter level, BlockPos pos) {
        return VanillaCrop.growthSpeed(crop, level, pos);
    }

    /** Never made: a crop only for the right to call vanilla's protected growth odds. */
    private static final class VanillaCrop extends CropBlock {
        private VanillaCrop() {
            super(null);
        }

        static float growthSpeed(BlockState crop, BlockGetter level, BlockPos pos) {
            return getGrowthSpeed(crop.getBlock(), level, pos);
        }
    }
}
