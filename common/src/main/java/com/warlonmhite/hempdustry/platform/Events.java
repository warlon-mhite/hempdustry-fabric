package com.warlonmhite.hempdustry.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import java.util.function.Consumer;

/** The game's moments the mod listens to. Each method adds a listener; call them while the mod starts. */
public interface Events {

    /** After every server tick. */
    void onServerTickEnd(Consumer<MinecraftServer> listener);

    /** Once the server has started and its registries are loaded. */
    void onServerStarted(Consumer<MinecraftServer> listener);

    /** Once the server has stopped. An integrated server stops whenever a world is closed. */
    void onServerStopped(Consumer<MinecraftServer> listener);

    /** After a player respawns, as the new player. {@code alive} is the End portal's return home, which is not a death. */
    void onPlayerRespawn(RespawnListener listener);

    /** Whenever the server builds its commands, at start and on every {@code /reload}. */
    void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> listener);

    /** For every loot table as it loads, at start and on every {@code /reload}. */
    void onLootTableLoad(LootTableListener listener);

    @FunctionalInterface
    interface RespawnListener {
        void onRespawn(ServerPlayer player, boolean alive);
    }

    @FunctionalInterface
    interface LootTableListener {
        /**
         * @param builtIn whether the table is the game's -- vanilla's, or one of Mojang's experiment
         *                packs' -- or a mod's own, as opposed to one a datapack put in its place
         *                ({@code LootTableOrigin})
         */
        void onLoad(ResourceKey<LootTable> key, boolean builtIn, HolderLookup.Provider registries, LootTableEditor table);
    }

    /** The two things the mod does to a table it did not write. */
    interface LootTableEditor {

        /** Adds a pool of its own, rolled after the table's own. */
        void addPool(LootPool.Builder pool);

        /** Adds {@code entry} to every pool the table already has, as one more choice inside each roll. */
        void addToEveryPool(LootPoolEntryContainer.Builder<?> entry);
    }
}
