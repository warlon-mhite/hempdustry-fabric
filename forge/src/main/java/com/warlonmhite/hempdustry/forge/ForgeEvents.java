package com.warlonmhite.hempdustry.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.forge.mixin.LootPoolAccessor;
import com.warlonmhite.hempdustry.forge.mixin.LootTableAccessor;
import com.warlonmhite.hempdustry.loot.LootTableOrigin;
import com.warlonmhite.hempdustry.platform.Events;
import com.warlonmhite.hempdustry.registry.DataRegistries;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** The mod's lifecycle hooks, on Forge 36's game event bus. */
public final class ForgeEvents implements Events {

    @Override
    public void onServerTickEnd(Consumer<MinecraftServer> listener) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                // 1.16.5's tick event carries no server; there is only ever one.
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    listener.accept(server);
                }
            }
        });
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> listener) {
        MinecraftForge.EVENT_BUS.addListener((FMLServerStartedEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> listener) {
        MinecraftForge.EVENT_BUS.addListener((FMLServerStartingEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        MinecraftForge.EVENT_BUS.addListener((FMLServerStoppedEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onPlayerRespawn(RespawnListener listener) {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                listener.onRespawn(player, event.isEndConquered());
            }
        });
    }

    @Override
    public void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> listener) {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> listener.accept(event.getDispatcher()));
    }

    /** The mod's own loot listeners, for the tables Forge loads without its event (ForgeHooksLootMixin). */
    private static final List<LootTableListener> LOOT_LISTENERS = new CopyOnWriteArrayList<>();

    @Override
    public void onLootTableLoad(LootTableListener listener) {
        // The strains the loot reads were loaded from the same resources just before the tables
        // (LootTablesMixin), so the server's registries are current here.
        LOOT_LISTENERS.add(listener);
        MinecraftForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> listener.onLoad(
                event.getName(), LootTableOrigin.fromTheGameOrAMod(event.getName()), DataRegistries.SERVER,
                new Editor(event.getTable())));
    }

    /**
     * A table Forge 36 read without firing its event -- any file not from vanilla's own pack, Forge's
     * {@code tall_grass} among them -- handed to the mod's listeners as the event would have.
     */
    public static void loadedWithoutEvent(ResourceLocation name, LootTable table) {
        boolean builtIn = LootTableOrigin.fromTheGameOrAMod(name);
        for (LootTableListener listener : LOOT_LISTENERS) {
            listener.onLoad(name, builtIn, DataRegistries.SERVER, new Editor(table));
        }
    }

    private static final class Editor implements LootTableEditor {
        private final LootTable table;
        private int added;

        Editor(LootTable table) {
            this.table = table;
        }

        @Override
        public void addPool(LootPool.Builder pool) {
            table.addPool(pool.name(Hempdustry.MOD_ID + ":added_" + added++).build());
        }

        @Override
        public void addToEveryPool(LootPoolEntryContainer.Builder<?> entry) {
            LootPoolEntryContainer built = entry.build();
            for (LootPool pool : ((LootTableAccessor) table).hempdustry$pools()) {
                ((LootPoolAccessor) pool).hempdustry$entries().add(built);
            }
        }
    }
}
