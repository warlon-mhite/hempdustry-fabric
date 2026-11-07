package com.warlonmhite.hempdustry.forge;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.platform.client.ClientServices;
import com.warlonmhite.hempdustry.registry.DataRegistries;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * Sends the mod's datapack registries -- strains, device stats, edible bundle, green-out -- to every
 * client: as it joins, and again after every {@code /reload}, which is when Forge's datapack sync event
 * fires. The newer lines' vanilla sends them itself; 1.16.5 cannot, so this is its packet. The client
 * holds what arrives in {@link DataRegistries#CLIENT}, which tinting, naming and the creative tab read.
 */
final class ForgeDataSync {

    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Hempdustry.MOD_ID, "data"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ForgeDataSync() {
    }

    static void register() {
        CHANNEL.registerMessage(0, Registries.class, Registries::write, Registries::read, Registries::handle);
        MinecraftForge.EVENT_BUS.addListener(ForgeDataSync::onDatapackSync);
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        Registries message = new Registries(DataRegistries.SERVER.write());
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        } else {
            CHANNEL.send(PacketDistributor.ALL.noArg(), message);
        }
    }

    /** What the server loaded, as {@link DataRegistries#write} wrote it. */
    static final class Registries {
        final CompoundTag tag;

        Registries(CompoundTag tag) {
            this.tag = tag;
        }

        void write(FriendlyByteBuf buf) {
            buf.writeNbt(tag);
        }

        static Registries read(FriendlyByteBuf buf) {
            CompoundTag tag = buf.readNbt();
            return new Registries(tag == null ? new CompoundTag() : tag);
        }

        void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> Client.received(tag));
            context.get().setPacketHandled(true);
        }
    }

    /** The client's half, loaded only on a client: a client leaving its server forgets what it was sent. */
    static final class Client {
        private Client() {
        }

        static void register() {
            MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedOutEvent event) ->
                    DataRegistries.CLIENT.clear());
        }

        /**
         * Takes what the server sent, then rebuilds the creative search, which vanilla builds once at
         * start-up -- before any server has sent a strain -- and so would list no strain's stacks.
         */
        static void received(CompoundTag tag) {
            DataRegistries.CLIENT.read(tag);
            ModItemGroups.clearSearchCache();
            ClientServices.CLIENT.refreshCreativeSearch();
        }
    }
}
