package com.warlonmhite.hempdustry.neoforge;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.neoforge.mixin.LootPoolAccessor;
import com.warlonmhite.hempdustry.neoforge.mixin.LootTableAccessor;
import com.warlonmhite.hempdustry.platform.Events;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;
import java.util.function.Consumer;

public final class NeoForgeEvents implements Events {
    /** The resources the loot tables are being read from, kept by {@code ReloadableServerRegistriesMixin}. */
    public static volatile ResourceManager lootResources;

    @Override
    public void onServerTickEnd(Consumer<MinecraftServer> listener) {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> listener) {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> listener.accept(event.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> listener) {
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> listener.accept(event.getServer()));
    }

    @Override
    public void onPlayerRespawn(RespawnListener listener) {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                listener.onRespawn(player, event.isEndConquered());
            }
        });
    }

    @Override
    public void onRegisterCommands(Consumer<CommandDispatcher<CommandSourceStack>> listener) {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> listener.accept(event.getDispatcher()));
    }

    @Override
    public void onLootTableLoad(LootTableListener listener) {
        NeoForge.EVENT_BUS.addListener(LootTableLoadEvent.class, event -> listener.onLoad(
                event.getKey(), builtIn(event.getName()), event.getRegistries(), new Editor(event.getTable())));
    }

    /**
     * Whether the copy of {@code table} that won came with the game or with a mod -- a pack the game
     * knows by name -- rather than from a datapack put in its place, which is left as its author wrote it.
     *
     * <p>Fabric answers the same question but counts the game's own experiment packs (Trade Rebalance)
     * as datapacks; NeoForge knows them by name too, so their chests get the mod's loot here.
     */
    private static boolean builtIn(ResourceLocation table) {
        ResourceManager resources = lootResources;
        if (resources == null) {
            return true;
        }
        Optional<Resource> file = resources.getResource(
                ResourceLocation.fromNamespaceAndPath(table.getNamespace(), "loot_table/" + table.getPath() + ".json"));
        return file.map(resource -> resource.source().location().knownPackInfo().isPresent()).orElse(true);
    }

    /** NeoForge hands over a table already built; a pool it adds needs a name of its own. */
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
                access.hempdustry$setEntries(ImmutableList.<LootPoolEntryContainer>builder()
                        .addAll(access.hempdustry$entries()).add(built).build());
            }
        }
    }
}
