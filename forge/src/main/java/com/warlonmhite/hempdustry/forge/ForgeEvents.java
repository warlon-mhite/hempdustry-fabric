package com.warlonmhite.hempdustry.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.forge.mixin.LootPoolAccessor;
import com.warlonmhite.hempdustry.forge.mixin.LootTableAccessor;
import com.warlonmhite.hempdustry.loot.LootTableOrigin;
import com.warlonmhite.hempdustry.platform.Events;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
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
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.apache.commons.lang3.ArrayUtils;

import java.util.function.Consumer;

public final class ForgeEvents implements Events {
    @Override
    public void onServerTickEnd(Consumer<MinecraftServer> listener) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                listener.accept(event.getServer());
            }
        });
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> listener) {
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> listener) {
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> listener.accept(event.getServer()));
    }

    @Override
    public void onPlayerRespawn(RespawnListener listener) {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                listener.onRespawn(player, event.isEndConquered());
            }
        });
    }

    @Override
    public void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> listener) {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> listener.accept(event.getDispatcher()));
    }

    @Override
    public void onLootTableLoad(LootTableListener listener) {
        // 1.20.1's event hands over no registries; the loot loads against the ones LootTableOrigin keeps.
        MinecraftForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> listener.onLoad(
                event.getName(), LootTableOrigin.fromTheGameOrAMod(event.getName()), registries(),
                new Editor(event.getTable())));
    }

    private static HolderLookup.Provider registries() {
        RegistryAccess registries = LootTableOrigin.registries;
        if (registries == null) {
            throw new IllegalStateException("Loot tables are loading with no registries kept");
        }
        return registries;
    }

    /** Forge hands over a table already built; a pool it adds needs a name of its own. */
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
                LootPoolAccessor access = (LootPoolAccessor) pool;
                access.hempdustry$setEntries(ArrayUtils.add(access.hempdustry$entries(), built));
            }
        }
    }
}
