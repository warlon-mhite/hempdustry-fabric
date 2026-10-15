package com.warlonmhite.hempdustry.neoforge;

import com.warlonmhite.hempdustry.platform.Menus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each menu type keeps the codec its data is read with on the client; the server writes the data
 * with the same one, looked up from the menu it has just made, so the two cannot disagree.
 */
public final class NeoForgeMenus implements Menus {
    private final Map<MenuType<?>, StreamCodec<? super RegistryFriendlyByteBuf, ?>> codecs = new ConcurrentHashMap<>();

    @Override
    public <T extends AbstractContainerMenu, D> MenuType<T> menuType(
            Factory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        MenuType<T> type = IMenuTypeExtension.create((containerId, inventory, data) ->
                factory.create(containerId, inventory, codec.decode(data)));
        codecs.put(type, codec);
        return type;
    }

    @Override
    public <D> void open(ServerPlayer player, MenuProvider provider, D data) {
        if (player.connection.hasChannel(AdvancedOpenScreenPayload.TYPE)) {
            player.openMenu(new WithData<>(provider, data), buffer -> {
            });
        } else {
            // A connection that never agreed NeoForge's channels -- a game test's mock player, another
            // mod's fake player -- has no NeoForge client at the far end to build the screen from the
            // data, and NeoForge refuses to send it. It gets the same menu, the vanilla way.
            player.openMenu(provider);
        }
    }

    @SuppressWarnings("unchecked")
    private <D> void write(AbstractContainerMenu menu, D data, RegistryFriendlyByteBuf buffer) {
        StreamCodec<? super RegistryFriendlyByteBuf, D> codec =
                (StreamCodec<? super RegistryFriendlyByteBuf, D>) codecs.get(menu.getType());
        if (codec == null) {
            throw new IllegalStateException("Menu type " + menu.getType() + " was not made to carry data");
        }
        codec.encode(buffer, data);
    }

    /** {@code provider} in every respect, writing {@code data} for the client once the menu exists. */
    private final class WithData<D> implements MenuProvider {
        private final MenuProvider provider;
        private final D data;

        WithData(MenuProvider provider, D data) {
            this.provider = provider;
            this.data = data;
        }

        @Override
        public Component getDisplayName() {
            return provider.getDisplayName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return provider.createMenu(containerId, inventory, player);
        }

        @Override
        public boolean shouldTriggerClientSideContainerClosingOnOpen() {
            return provider.shouldTriggerClientSideContainerClosingOnOpen();
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            write(menu, data, buffer);
        }
    }
}
