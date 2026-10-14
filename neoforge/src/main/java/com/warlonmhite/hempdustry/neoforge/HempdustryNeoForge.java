package com.warlonmhite.hempdustry.neoforge;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.neoforge.data.NeoForgeData;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/** NeoForge's entry point: the mod, as the rest of the code registers it, at the moments NeoForge sets. */
@Mod(Hempdustry.MOD_ID)
public final class HempdustryNeoForge {
    public HempdustryNeoForge(IEventBus modBus) {
        NeoForgeRegistration registration = (NeoForgeRegistration) Services.REGISTRATION;
        // NeoForge asks for the datapack registries before any registry event.
        modBus.addListener(DataPackRegistryEvent.NewRegistry.class, event -> {
            Hempdustry.registerDynamicRegistries();
            registration.onNewDatapackRegistries(event);
        });
        // The game's registries are open only while NeoForge registers, and they stay open until the
        // last registry event, so the mod's holders -- plain static fields, as on Fabric -- are first
        // touched from inside the first of those events, the block registry's, in Hempdustry.init's order.
        modBus.addListener(RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                NeoForgePlayerData.register();
                Hempdustry.init();
            }
        });
        modBus.addListener(BlockEntityTypeAddBlocksEvent.class, registration::onAddBlocks);
        modBus.addListener(RegisterCapabilitiesEvent.class, HempdustryNeoForge::exposeContainers);
        modBus.addListener(GatherDataEvent.Client.class, NeoForgeData::gather);
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, registration::onDatapackSync);
    }

    /**
     * Every block entity of the mod that holds items answers a pipe, a hopper or a storage network the
     * way a vanilla container does, sided where it says so. Fabric's transfer API wraps every container
     * on its own; NeoForge's only vanilla's, so the mod's are listed here, by registry rather than by
     * name, so a new machine is covered the day it registers.
     */
    private static void exposeContainers(RegisterCapabilitiesEvent event) {
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).getNamespace().equals(Hempdustry.MOD_ID)) {
                expose(event, type);
            }
        }
    }

    private static <T extends BlockEntity> void expose(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, type, (blockEntity, side) ->
                blockEntity instanceof WorldlyContainer sided ? new WorldlyContainerWrapper(sided, side)
                        : blockEntity instanceof Container container ? VanillaContainerWrapper.of(container)
                        : null);
    }
}
