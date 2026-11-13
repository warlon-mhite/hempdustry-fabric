package com.warlonmhite.hempdustry.test.forge;

import net.minecraftforge.registries.DataPackRegistriesHooks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraft.world.level.block.ComposterBlock;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.api.AfterEatEvent;
import com.warlonmhite.hempdustry.api.AfterInfuseEvent;
import com.warlonmhite.hempdustry.api.AfterSmokeEvent;
import com.warlonmhite.hempdustry.api.AllowSmokeEvent;
import com.warlonmhite.hempdustry.test.TestPlatform;
import com.google.gson.JsonElement;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.nio.file.Path;
import java.util.List;

public final class ForgeTestPlatform implements TestPlatform {
    private AddonProbe probe;
    private CropGrowthProbe growth;

    @Override
    public String loaderName() {
        return "forge";
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
        return roots(ForgeGameTests.MOD_ID);
    }

    @Override
    public LootTable lootTable(ResourceLocation id, JsonElement file) {
        return ForgeHooks.loadLootTable(LootDataType.TABLE.parser(), id, file, false);
    }

    @Override
    public float compostChance(ItemLike item) {
        // Vanilla's own table, which Forge 47's composter reads.
        return ComposterBlock.COMPOSTABLES.getOrDefault(item.asItem(), -1.0F);
    }

    @Override
    public int burnTime(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    @Override
    public Flammability flammability(Block block) {
        // Vanilla's table, which Forge's fire reads unless a block says otherwise.
        FireBlock fire = (FireBlock) Blocks.FIRE;
        BlockState state = block.defaultBlockState();
        return new Flammability(fire.getIgniteOdds(state), fire.getBurnOdds(state));
    }

    @Override
    public synchronized AddonProbe addonProbe() {
        if (probe == null) {
            AddonProbe p = new AddonProbe();
            MinecraftForge.EVENT_BUS.addListener((AllowSmokeEvent event) -> {
                p.heard("ALLOW_SMOKE", event.getPlayer(), event.getContents(), event.getDevice());
                if (p.refusing()) {
                    event.setCanceled(true);
                }
            });
            MinecraftForge.EVENT_BUS.addListener(
                    (AfterSmokeEvent event) -> p.heard("AFTER_SMOKE", event.getPlayer(), event.getContents(), event.getDevice()));
            MinecraftForge.EVENT_BUS.addListener(
                    (AfterEatEvent event) -> p.heard("AFTER_EAT", event.getPlayer(), event.getTier(), event.getQuality()));
            MinecraftForge.EVENT_BUS.addListener(
                    (AfterInfuseEvent event) -> p.heard("AFTER_INFUSE", event.getLevel(), event.getPos(), event.getStrength(), event.getQuality()));
            probe = p;
        }
        return probe;
    }

    @Override
    public synchronized CropGrowthProbe cropGrowthProbe() {
        if (growth == null) {
            CropGrowthProbe p = new CropGrowthProbe();
            MinecraftForge.EVENT_BUS.addListener((BlockEvent.CropGrowEvent.Pre event) -> {
                p.heard("PRE", event.getPos());
                if (p.refusing(event.getPos())) {
                    event.setResult(Event.Result.DENY);
                }
            });
            MinecraftForge.EVENT_BUS.addListener((BlockEvent.CropGrowEvent.Post event) -> p.heard("POST", event.getPos()));
            growth = p;
        }
        return growth;
    }

    private static List<Path> roots(String modId) {
        return List.of(ModList.get().getModFileById(modId).getFile().getSecureJar().getRootPath());
    }
}
