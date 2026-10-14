package com.warlonmhite.hempdustry.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.warlonmhite.hempdustry.platform.Events;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import java.util.function.Consumer;

public final class FabricEvents implements Events {
    @Override
    public void onServerTickEnd(Consumer<MinecraftServer> listener) {
        ServerTickEvents.END_SERVER_TICK.register(listener::accept);
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STARTED.register(listener::accept);
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STOPPED.register(listener::accept);
    }

    @Override
    public void onPlayerRespawn(RespawnListener listener) {
        ServerPlayerEvents.AFTER_RESPAWN.register(listener::onRespawn);
    }

    @Override
    public void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> listener) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> listener.accept(dispatcher));
    }

    @Override
    public void onLootTableLoad(LootTableListener listener) {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) ->
                listener.onLoad(key, source.isBuiltin(), registries, new Editor(tableBuilder)));
    }

    private record Editor(LootTable.Builder table) implements LootTableEditor {
        @Override
        public void addPool(LootPool.Builder pool) {
            table.withPool(pool);
        }

        @Override
        public void addToEveryPool(LootPoolEntryContainer.Builder<?> entry) {
            table.modifyPools(pool -> pool.add(entry));
        }
    }
}
