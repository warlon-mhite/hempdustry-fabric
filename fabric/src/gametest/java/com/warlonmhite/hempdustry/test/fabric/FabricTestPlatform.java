package com.warlonmhite.hempdustry.test.fabric;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.api.HempdustryEvents;
import com.warlonmhite.hempdustry.test.HempdustryGameTests;
import com.warlonmhite.hempdustry.test.TestPlatform;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;

import java.nio.file.Path;
import java.util.List;

public final class FabricTestPlatform implements TestPlatform {
    private AddonProbe probe;

    @Override
    public String loaderName() {
        return "fabric";
    }

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
        // Fabric's registry writes vanilla's own table, which the composter reads.
        return ComposterBlock.COMPOSTABLES.getOrDefault(item.asItem(), -1.0F);
    }

    @Override
    public int burnTime(ItemStack stack) {
        Integer ticks = FuelRegistry.INSTANCE.get(stack.getItem());
        return ticks == null ? 0 : ticks;
    }

    @Override
    public Flammability flammability(Block block) {
        // Fabric calls them burn and spread: the odds of catching fire, and of burning away.
        FlammableBlockRegistry.Entry entry = FlammableBlockRegistry.getDefaultInstance().get(block);
        return entry == null ? new Flammability(0, 0) : new Flammability(entry.getBurnChance(), entry.getSpreadChance());
    }

    @Override
    public synchronized AddonProbe addonProbe() {
        if (probe == null) {
            AddonProbe p = new AddonProbe();
            HempdustryEvents.ALLOW_SMOKE.register((player, contents, device) -> {
                p.heard("ALLOW_SMOKE", player, contents, device);
                return !p.refusing();
            });
            HempdustryEvents.AFTER_SMOKE.register((player, contents, device) -> p.heard("AFTER_SMOKE", player, contents, device));
            HempdustryEvents.AFTER_EAT.register((player, tier, quality) -> p.heard("AFTER_EAT", player, tier, quality));
            HempdustryEvents.AFTER_INFUSE.register((world, pos, strength, quality) -> p.heard("AFTER_INFUSE", world, pos, strength, quality));
            probe = p;
        }
        return probe;
    }

    private static List<Path> roots(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).orElseThrow().getRootPaths();
    }
}
