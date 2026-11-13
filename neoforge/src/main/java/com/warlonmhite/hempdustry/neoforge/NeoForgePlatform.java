package com.warlonmhite.hempdustry.neoforge;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.CommonHooks;

import java.nio.file.Path;

public final class NeoForgePlatform implements Platform {
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
        return "neoforge";
    }

    @Override
    public float cropGrowthSpeed(BlockState crop, BlockGetter level, BlockPos pos) {
        return VanillaCrop.growthSpeed(crop, level, pos);
    }

    /** NeoForge's own form of the stare test's disguise, which asks every mask, and its anger event. */
    @Override
    public boolean disguisedFrom(Player player, Enderman enderman) {
        return !LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET.test(player, enderman)
                || CommonHooks.shouldSuppressEnderManAnger(enderman, player);
    }

    @Override
    public boolean canCropGrow(Level level, BlockPos pos, BlockState state, boolean roll) {
        return CommonHooks.canCropGrow(level, pos, state, roll);
    }

    @Override
    public void cropGrew(Level level, BlockPos pos, BlockState before) {
        CommonHooks.fireCropGrowPost(level, pos, before);
    }

    /** Never made: a crop only for the right to call the protected growth odds, NeoForge's form of them. */
    private static final class VanillaCrop extends CropBlock {
        private VanillaCrop() {
            super(null);
        }

        static float growthSpeed(BlockState crop, BlockGetter level, BlockPos pos) {
            return getGrowthSpeed(crop, level, pos);
        }
    }
}
