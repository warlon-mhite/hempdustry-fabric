package com.warlonmhite.hempdustry.test.forge;

import com.google.common.collect.ImmutableList;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.api.AfterEatEvent;
import com.warlonmhite.hempdustry.api.AfterInfuseEvent;
import com.warlonmhite.hempdustry.api.AfterSmokeEvent;
import com.warlonmhite.hempdustry.api.AllowSmokeEvent;
import com.warlonmhite.hempdustry.test.HempdustryGameTests;
import com.warlonmhite.hempdustry.test.TestPlatform;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;
import java.util.List;

public final class ForgeTestPlatform implements TestPlatform {
    private AddonProbe probe;

    @Override
    public String loaderName() {
        return "forge";
    }

    @Override
    public net.minecraft.world.level.storage.loot.LootTable decodeLootTable(net.minecraft.server.MinecraftServer server,
            net.minecraft.resources.ResourceLocation id, com.google.gson.JsonElement json) {
        // custom: a mod's or a data pack's table, as LootTables#apply reads every file outside vanilla's own
        return ForgeHooks.loadLootTable(net.minecraft.world.level.storage.loot.Deserializers.createLootTableSerializer().create(),
                id, json, true, server.getLootTables());
    }

    @Override
    public void markAsHandshaken(Connection connection) {
        // What FMLHandshakeHandler leaves on a channel once a modded client has logged in; Forge 36's
        // player list reads it as the player joins (NetworkHooks#getConnectionType), and finds null on
        // a channel no handshake ran on.
        connection.channel().attr(AttributeKey.<String>valueOf("fml:netversion")).set(FMLNetworkConstants.NETVERSION);
    }

    @Override
    public List<Path> modRoots() {
        return roots(Hempdustry.MOD_ID, Hempdustry.class);
    }

    @Override
    public List<Path> testModRoots() {
        return roots(ForgeGameTests.MOD_ID, HempdustryGameTests.class);
    }

    @Override
    public float compostChance(ItemLike item) {
        // Vanilla's own table, which Forge 36's composter reads.
        return ComposterBlock.COMPOSTABLES.getOrDefault(item.asItem(), -1.0F);
    }

    @Override
    public int burnTime(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack);
    }

    @Override
    public Flammability flammability(Block block) {
        // Vanilla's table, which Forge's fire reads unless a block says otherwise; 1.16.5 calls the
        // ignite odds flame odds.
        FireBlock fire = (FireBlock) Blocks.FIRE;
        BlockState state = block.defaultBlockState();
        return new Flammability(fire.getFlameOdds(state), fire.getBurnOdd(state));
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

    /**
     * Where a mod's files are read from: the root its manifest sits in and the root one of its classes
     * sits in -- two folders of a dev run, or the same jar. Forge 36's class loader names no code
     * source, so the classes' root is found through the mod's file as well.
     */
    private static List<Path> roots(String modId, Class<?> member) {
        ModFile file = ModList.get().getModFileById(modId).getFile();
        Path manifest = file.findResource("META-INF/mods.toml");
        Path type = file.findResource(member.getName().replace('.', '/') + ".class");
        Path classes = type;
        for (int i = 0; i < member.getName().split("\\.").length; i++) {
            classes = classes.getParent();
        }
        Path resources = manifest.getParent().getParent();
        return resources.equals(classes) ? ImmutableList.of(resources) : ImmutableList.of(resources, classes);
    }
}
