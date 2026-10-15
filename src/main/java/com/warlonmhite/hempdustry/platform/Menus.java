package com.warlonmhite.hempdustry.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Menus that carry data to the client when they open — for the mod's machines, the block's
 * position, which is how a screen finds the block entity it belongs to. Vanilla's menus carry none.
 */
public interface Menus {

    /** A menu type whose client side is built from {@code D}, read off the wire with {@code codec}. */
    <T extends AbstractContainerMenu, D> MenuType<T> menuType(
            Factory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec);

    /** Opens {@code provider}'s menu for {@code player}, sending {@code data} along to the client. */
    <D> void open(ServerPlayer player, MenuProvider provider, D data);

    @FunctionalInterface
    interface Factory<T extends AbstractContainerMenu, D> {
        T create(int containerId, Inventory inventory, D data);
    }
}
