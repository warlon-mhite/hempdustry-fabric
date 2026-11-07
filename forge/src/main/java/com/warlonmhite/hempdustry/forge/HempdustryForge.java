package com.warlonmhite.hempdustry.forge;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.forge.client.HempdustryForgeClient;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.registry.DataRegistries;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLModIdMappingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The mod on Forge 36: Minecraft 1.16.5. */
@Mod(Hempdustry.MOD_ID)
public final class HempdustryForge {

    private static final ResourceLocation ITEM_HANDLER = new ResourceLocation(Hempdustry.MOD_ID, "items");

    public HempdustryForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeRegistration registration = (ForgeRegistration) Services.REGISTRATION;
        // The datapack registries first: everything registered after may read them.
        Hempdustry.registerDynamicRegistries();
        // The mod's holders are plain static fields, registered in one pass in Hempdustry.init's order,
        // which is first touched from inside the first registry event, the block registry's. Forge 36
        // opens each of its registries only during that registry's own event, so the pass opens them all
        // for its length and closes each again; each still gets its own event afterwards, and is opened
        // and closed by Forge for it as usual.
        modBus.addGenericListener(Block.class, EventPriority.HIGHEST, (RegistryEvent.Register<Block> event) -> {
            ForgePlayerData.register();
            withEveryRegistryOpen(() -> {
                Hempdustry.init();
                ModItemGroups.assignTab();
            });
        });
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(Hempdustry::setup));
        ForgeDataSync.register();
        // Before any advancement is read: the converted data names several items in one predicate.
        AnyOfItemPredicate.register();
        // A world from GanjaCraft, the mod this one grew out of, opens as a Hempdustry world.
        GanjaCraftMappings.register();
        modBus.addListener((FMLModIdMappingEvent event) -> registration.onIdMapping());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            HempdustryForgeClient.init(modBus);
            ForgeDataSync.Client.register();
        }
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, HempdustryForge::exposeContainers);
        MinecraftForge.EVENT_BUS.addListener((FurnaceFuelBurnTimeEvent event) -> registration.burnTime(event));
        MinecraftForge.EVENT_BUS.addListener(registration::loadBiome);
        // The datapack registries load with the server's data, as the newer lines' vanilla loads them.
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) ->
                DataRegistries.reloadListeners().forEach(event::addListener));
    }

    /**
     * Runs {@code work} with every registry open to vanilla's {@code Registry.register}, which the mod's
     * holders call. Forge 36 keeps two locks on each of its registries: the registry's own, open only
     * during that registry's event, and the vanilla-facing wrapper's, closed for good once vanilla has
     * registered its content, so that mods go through Forge's own register methods. Both are opened
     * for the pass and closed again, so each registry still gets its own event afterwards and is
     * opened and closed by Forge for it as usual.
     */
    private static void withEveryRegistryOpen(Runnable work) {
        List<ForgeRegistry<?>> opened = new ArrayList<>();
        List<Registry<?>> unlocked = new ArrayList<>();
        try {
            for (ResourceLocation key : Registry.REGISTRY.keySet()) {
                ForgeRegistry<?> registry = RegistryManager.ACTIVE.getRegistry(key);
                if (registry != null && !key.equals(Registry.BLOCK_REGISTRY.location())) {
                    registry.unfreeze();
                    opened.add(registry);
                }
                Registry<?> wrapper = Registry.REGISTRY.get(key);
                if (wrapper != null && setWrapperLock(wrapper, false)) {
                    unlocked.add(wrapper);
                }
            }
            work.run();
        } finally {
            unlocked.forEach(wrapper -> setWrapperLock(wrapper, true));
            opened.forEach(ForgeRegistry::freeze);
        }
    }

    /** Sets a Forge registry wrapper's lock; false for a registry that is not one. */
    private static boolean setWrapperLock(Registry<?> wrapper, boolean locked) {
        Field lock = WRAPPER_LOCKS.get(wrapper.getClass());
        if (lock == null) {
            return false;
        }
        try {
            lock.setBoolean(wrapper, locked);
            return true;
        } catch (IllegalAccessException impossible) {
            throw new IllegalStateException("Could not open " + wrapper, impossible);
        }
    }

    /** Forge 36's two wrappers -- plain, and defaulted (blocks, items) -- each with its own lock. */
    private static final Map<Class<?>, Field> WRAPPER_LOCKS = new java.util.HashMap<>();

    static {
        for (String name : new String[] {"net.minecraftforge.registries.NamespacedWrapper",
                "net.minecraftforge.registries.NamespacedDefaultedWrapper"}) {
            try {
                Field lock = Class.forName(name).getDeclaredField("locked");
                lock.setAccessible(true);
                WRAPPER_LOCKS.put(lock.getDeclaringClass(), lock);
            } catch (ReflectiveOperationException missing) {
                throw new IllegalStateException("Forge's registry wrapper " + name + " is not where Forge 36 keeps it", missing);
            }
        }
    }

    /**
     * Forge's item-handler capability on every container block entity of ours, so pipes and hoppers of
     * other mods reach the machines as they reach vanilla's -- Forge gives it only to vanilla's.
     */
    private static void exposeContainers(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        ResourceLocation type = Registry.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        if (type == null || !type.getNamespace().equals(Hempdustry.MOD_ID) || !(blockEntity instanceof Container container)) {
            return;
        }
        ItemHandlers handlers = new ItemHandlers(container);
        event.addCapability(ITEM_HANDLER, handlers);
        event.addListener(handlers::invalidate);
    }

    private static final class ItemHandlers implements ICapabilityProvider {
        private final Container container;
        private final Map<Direction, LazyOptional<IItemHandler>> sided = new EnumMap<>(Direction.class);
        private final LazyOptional<IItemHandler> unsided;

        ItemHandlers(Container container) {
            this.container = container;
            this.unsided = LazyOptional.of(() -> new InvWrapper(container));
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            if (capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
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
