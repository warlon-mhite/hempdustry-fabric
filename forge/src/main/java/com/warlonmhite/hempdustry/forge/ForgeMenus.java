package com.warlonmhite.hempdustry.forge;

import com.warlonmhite.hempdustry.platform.Menus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.Connection;

public final class ForgeMenus implements Menus {
    @Override
    public <T extends AbstractContainerMenu, D> MenuType<T> menuType(Factory<T, D> factory, Data<D> codec) {
        return IForgeMenuType.create((containerId, inventory, buf) -> factory.create(containerId, inventory, codec.read(buf)));
    }

    /**
     * Whether Forge's channels were agreed on this connection. One that never shook hands at all
     * (a game test's mock player, another mod's fake one) has no version to ask, which Forge's own
     * check does not expect.
     */
    private static boolean speaksForge(Connection connection) {
        // The handshake leaves its data on the channel; a vanilla client's, or none at all, has none.
        return connection.channel() != null && NetworkHooks.getConnectionData(connection) != null;
    }

    @Override
    public <D> void open(ServerPlayer player, MenuProvider provider, Data<D> codec, D data) {
        if (!speaksForge(player.connection.connection)) {
            // A connection that never agreed Forge's channels -- a game test's mock player, another
            // mod's fake player -- has no Forge client at the far end to build the screen from the
            // data. It gets the same menu, the vanilla way.
            player.openMenu(provider);
        } else {
            NetworkHooks.openGui(player, provider, buf -> codec.write(buf, data));
        }
    }
}
