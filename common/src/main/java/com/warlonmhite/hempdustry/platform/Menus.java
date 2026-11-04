package com.warlonmhite.hempdustry.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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
            Factory<T, D> factory, Data<D> codec);

    /** Opens {@code provider}'s menu for {@code player}, sending {@code data} along to the client, written with {@code codec}. */
    <D> void open(ServerPlayer player, MenuProvider provider, Data<D> codec, D data);

    /** How {@code D} is written to the menu-opening packet and read back: a buffer on both loaders on 1.20.1. */
    interface Data<D> {
        Data<BlockPos> BLOCK_POS = new Data<>() {
            @Override
            public void write(FriendlyByteBuf buf, BlockPos pos) {
                buf.writeBlockPos(pos);
            }

            @Override
            public BlockPos read(FriendlyByteBuf buf) {
                return buf.readBlockPos();
            }
        };

        void write(FriendlyByteBuf buf, D data);

        D read(FriendlyByteBuf buf);
    }

    @FunctionalInterface
    interface Factory<T extends AbstractContainerMenu, D> {
        T create(int containerId, Inventory inventory, D data);
    }
}
