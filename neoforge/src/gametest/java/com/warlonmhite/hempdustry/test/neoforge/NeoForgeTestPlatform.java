package com.warlonmhite.hempdustry.test.neoforge;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.api.AfterEatEvent;
import com.warlonmhite.hempdustry.api.AfterInfuseEvent;
import com.warlonmhite.hempdustry.api.AfterSmokeEvent;
import com.warlonmhite.hempdustry.api.AllowSmokeEvent;
import com.warlonmhite.hempdustry.test.TestPlatform;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.nio.file.Path;
import java.util.List;

public final class NeoForgeTestPlatform implements TestPlatform {
    private AddonProbe probe;

    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public List<RegistryDataLoader.RegistryData<?>> dynamicRegistries() {
        return DataPackRegistriesHooks.getDataPackRegistries();
    }

    @Override
    public List<Path> modRoots() {
        return roots(Hempdustry.MOD_ID);
    }

    @Override
    public List<Path> testModRoots() {
        return roots(NeoForgeGameTests.MOD_ID);
    }

    @Override
    public float compostChance(ItemLike item) {
        Compostable compostable = item.asItem().builtInRegistryHolder().getData(NeoForgeDataMaps.COMPOSTABLES);
        return compostable == null ? -1.0F : compostable.chance();
    }

    @Override
    public Flammability flammability(Block block) {
        // Vanilla's table, which NeoForge's fire reads unless a block says otherwise.
        FireBlock fire = (FireBlock) Blocks.FIRE;
        BlockState state = block.defaultBlockState();
        return new Flammability(fire.getIgniteOdds(state), fire.getBurnOdds(state));
    }

    @Override
    public synchronized AddonProbe addonProbe() {
        if (probe == null) {
            AddonProbe p = new AddonProbe();
            NeoForge.EVENT_BUS.addListener(AllowSmokeEvent.class, event -> {
                p.heard("ALLOW_SMOKE", event.getPlayer(), event.getContents(), event.getDevice());
                if (p.refusing()) {
                    event.setCanceled(true);
                }
            });
            NeoForge.EVENT_BUS.addListener(AfterSmokeEvent.class,
                    event -> p.heard("AFTER_SMOKE", event.getPlayer(), event.getContents(), event.getDevice()));
            NeoForge.EVENT_BUS.addListener(AfterEatEvent.class,
                    event -> p.heard("AFTER_EAT", event.getPlayer(), event.getTier(), event.getQuality()));
            NeoForge.EVENT_BUS.addListener(AfterInfuseEvent.class,
                    event -> p.heard("AFTER_INFUSE", event.getLevel(), event.getPos(), event.getStrength(), event.getQuality()));
            probe = p;
        }
        return probe;
    }

    private static List<Path> roots(String modId) {
        return List.of(ModList.get().getModFileById(modId).getFile().getSecureJar().getRootPath());
    }
}
