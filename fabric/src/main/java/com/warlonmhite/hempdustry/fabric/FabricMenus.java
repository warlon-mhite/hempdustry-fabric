package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.platform.Menus;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class FabricMenus implements Menus {
    @Override
    public <T extends AbstractContainerMenu, D> MenuType<T> menuType(Factory<T, D> factory, Data<D> codec) {
        return new ExtendedScreenHandlerType<>((containerId, inventory, buf) ->
                factory.create(containerId, inventory, codec.read(buf)));
    }

    @Override
    public <D> void open(ServerPlayer player, MenuProvider provider, Data<D> codec, D data) {
        player.openMenu(new WithData<>(provider, codec, data));
    }

    /** {@code provider} in every respect, with {@code data} to write to the opening packet. */
    private record WithData<D>(MenuProvider provider, Data<D> codec, D data) implements ExtendedScreenHandlerFactory {
        @Override
        public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
            codec.write(buf, data);
        }

        @Override
        public Component getDisplayName() {
            return provider.getDisplayName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return provider.createMenu(containerId, inventory, player);
        }
    }
}
