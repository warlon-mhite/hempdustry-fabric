package com.warlonmhite.hempdustry.forge;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import java.util.List;
import java.util.ArrayList;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.ForgeRegistry;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.forge.data.ForgeData;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import com.warlonmhite.hempdustry.forge.client.HempdustryForgeClient;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** Forge's entry point: the mod, as the rest of the code registers it, at the moments Forge sets. */
@Mod(Hempdustry.MOD_ID)
public final class HempdustryForge {
    private static final ResourceLocation ITEM_HANDLER = new ResourceLocation(Hempdustry.MOD_ID, "items");

    public HempdustryForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeRegistration registration = (ForgeRegistration) Services.REGISTRATION;
        // Forge asks for the datapack registries before any registry event.
        modBus.addListener((DataPackRegistryEvent.NewRegistry event) -> {
            Hempdustry.registerDynamicRegistries();
            registration.onNewDatapackRegistries(event);
        });
        // The mod's holders are plain static fields, as on Fabric, registered in one pass in
        // Hempdustry.init's order, which is first touched from inside the first registry event, the
        // block registry's. Forge 47 opens each of its registries only during that registry's own
        // event, so the pass opens them all for its length and closes each again; each still gets its
        // own event afterwards, and is opened and closed by Forge for it as usual.
        modBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                ForgePlayerData.register();
                withEveryRegistryOpen(Hempdustry::init);
            }
        });
        modBus.addListener(ForgeData::gather);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(Hempdustry::setup));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            HempdustryForgeClient.init(modBus);
        }
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, HempdustryForge::exposeContainers);
        MinecraftForge.EVENT_BUS.addListener((FurnaceFuelBurnTimeEvent event) -> registration.burnTime(event));
    }

    private static void withEveryRegistryOpen(Runnable work) {
        List<ForgeRegistry<?>> opened = new ArrayList<>();
        for (ResourceLocation key : BuiltInRegistries.REGISTRY.keySet()) {
            ForgeRegistry<?> registry = RegistryManager.ACTIVE.getRegistry(key);
            if (registry != null && !key.equals(Registries.BLOCK.location())) {
                registry.unfreeze();
                opened.add(registry);
            }
        }
        try {
            work.run();
        } finally {
            opened.forEach(ForgeRegistry::freeze);
        }
    }

    /**
     * Every block entity of the mod that holds items answers a pipe, a hopper or a storage network the
     * way a vanilla container does, sided where it says so. Fabric's transfer API wraps every container
     * on its own; Forge's only vanilla's, so the mod's own are given the capability here, by namespace
     * rather than by name, so a new machine is covered the day it registers.
     */
    private static void exposeContainers(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        ResourceLocation type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        if (type == null || !type.getNamespace().equals(Hempdustry.MOD_ID) || !(blockEntity instanceof Container container)) {
            return;
        }
        ItemHandlers handlers = new ItemHandlers(container);
        event.addCapability(ITEM_HANDLER, handlers);
        event.addListener(handlers::invalidate);
    }

    /** The item handlers of one container: one per side when it is sided, one for all otherwise. */
    private static final class ItemHandlers implements ICapabilityProvider {
        private final Container container;
        private final Map<Direction, LazyOptional<IItemHandler>> sided = new EnumMap<>(Direction.class);
        private LazyOptional<IItemHandler> unsided;

        ItemHandlers(Container container) {
            this.container = container;
            this.unsided = LazyOptional.of(() -> new InvWrapper(container));
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            if (capability != ForgeCapabilities.ITEM_HANDLER) {
                return LazyOptional.empty();
            }
            if (side != null && container instanceof WorldlyContainer worldly) {
                return sided.computeIfAbsent(side, face -> LazyOptional.of(() -> new SidedInvWrapper(worldly, face))).cast();
            }
            return unsided.cast();
        }

        void invalidate() {
            unsided.invalidate();
            sided.values().forEach(LazyOptional::invalidate);
        }
    }
}
