package com.warlonmhite.hempdustry.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.warlonmhite.hempdustry.loot.LootTableOrigin;
import com.warlonmhite.hempdustry.platform.Events;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
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
    public void onServerStarting(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STARTING.register(listener::accept);
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        ServerLifecycleEvents.SERVER_STOPPED.register(listener::accept);
    }

    @Override
    public void onPlayerRespawn(RespawnListener listener) {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> listener.onRespawn(newPlayer, alive));
    }

    @Override
    public void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> listener) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> listener.accept(dispatcher));
    }

    @Override
    public void onLootTableLoad(LootTableListener listener) {
        // 1.20.1's event hands over no registries; the loot loads against the ones LootTableOrigin keeps.
        LootTableEvents.MODIFY.register((resources, lootManager, key, tableBuilder, source) ->
                listener.onLoad(key, fromTheGameOrAMod(key, source), registries(), new Editor(tableBuilder)));
    }

    private static HolderLookup.Provider registries() {
        RegistryAccess registries = LootTableOrigin.registries;
        if (registries == null) {
            throw new IllegalStateException("Loot tables are loading with no registries kept");
        }
        return registries;
    }

    /**
     * Fabric's own answer, with one gap filled: it files every pack but vanilla's main one and the
     * mods' as a datapack, the game's feature packs among them, so a table it calls a datapack's is
     * asked the rule both loaders share. Another mod's replacement made in code is left
     * as that mod made it, as a datapack's is.
     */
    private static boolean fromTheGameOrAMod(ResourceLocation key, LootTableSource source) {
        return switch (source) {
            case VANILLA, MOD -> true;
            case REPLACED -> false;
            case DATA_PACK -> LootTableOrigin.fromTheGameOrAMod(key);
        };
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
