package com.warlonmhite.hempdustry.forge;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class ForgePlatform implements Platform {
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String modVersion() {
        return ModList.get().getModContainerById(Hempdustry.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0.0.0");
    }

    @Override
    public String loaderName() {
        return "forge";
    }

    @Override
    public float cropGrowthSpeed(BlockState crop, BlockGetter level, BlockPos pos) {
        return VanillaCrop.growthSpeed(crop, level, pos);
    }

    /** Never made: a crop only for the right to call the protected growth odds, Forge's form of them. */
    private static final class VanillaCrop extends CropBlock {
        private VanillaCrop() {
            super(null);
        }

        static float growthSpeed(BlockState crop, BlockGetter level, BlockPos pos) {
            return getGrowthSpeed(crop.getBlock(), level, pos);
        }
    }
}
