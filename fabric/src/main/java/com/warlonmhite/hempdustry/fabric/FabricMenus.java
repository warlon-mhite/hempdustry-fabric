package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.platform.Menus;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class FabricMenus implements Menus {
    @Override
    public <T extends AbstractContainerMenu, D> MenuType<T> menuType(
            Factory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        return new ExtendedMenuType<>(factory::create, codec);
    }

    @Override
    public <D> void open(ServerPlayer player, MenuProvider provider, D data) {
        player.openMenu(new WithData<>(provider, data));
    }

    /** {@code provider} in every respect, with {@code data} to write to the opening packet. */
    private record WithData<D>(MenuProvider provider, D data) implements ExtendedMenuProvider<D> {
        @Override
        public D getScreenOpeningData(ServerPlayer player) {
            return data;
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
        public boolean shouldCloseCurrentScreen() {
            return provider.shouldCloseCurrentScreen();
        }
    }
}
