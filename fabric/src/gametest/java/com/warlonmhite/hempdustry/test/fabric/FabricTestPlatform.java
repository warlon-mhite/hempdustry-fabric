package com.warlonmhite.hempdustry.test.fabric;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.test.HempdustryGameTests;
import com.warlonmhite.hempdustry.test.TestPlatform;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;

import java.nio.file.Path;
import java.util.List;

public final class FabricTestPlatform implements TestPlatform {
    @Override
    public List<RegistryDataLoader.RegistryData<?>> dynamicRegistries() {
        return DynamicRegistries.getDynamicRegistries();
    }

    @Override
    public List<Path> modRoots() {
        return roots(Hempdustry.MOD_ID);
    }

    @Override
    public List<Path> testModRoots() {
        return roots(HempdustryGameTests.NAMESPACE);
    }

    @Override
    public float compostChance(ItemLike item) {
        // Fabric's composting registry writes into vanilla's table, whose missing value is -1.
        return ComposterBlock.COMPOSTABLES.getFloat(item.asItem());
    }

    @Override
    public Flammability flammability(Block block) {
        // Fabric calls them burn and spread: the odds of catching fire, and of burning away.
        FlammableBlockRegistry.Entry entry = FlammableBlockRegistry.getDefaultInstance().get(block);
        return entry == null ? new Flammability(0, 0) : new Flammability(entry.getBurnChance(), entry.getSpreadChance());
    }

    private static List<Path> roots(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).orElseThrow().getRootPaths();
    }
}
