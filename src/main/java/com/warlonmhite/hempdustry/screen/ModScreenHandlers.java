package com.warlonmhite.hempdustry.screen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreenHandler;
import com.warlonmhite.hempdustry.screen.custom.HempPressScreenHandler;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    /**
     * Extended rather than plain so the block's position rides along to the client, which is what
     * lets the screen resolve the block entity it belongs to.
     */
    public static final MenuType<DecarboxylatorScreenHandler> DECARBOXYLATOR =
            Registry.register(BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "decarboxylator"),
                    Services.MENUS.menuType(DecarboxylatorScreenHandler::new, BlockPos.STREAM_CODEC));

    public static final MenuType<InfuserScreenHandler> INFUSER =
            Registry.register(BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "infuser"),
                    Services.MENUS.menuType(InfuserScreenHandler::new, BlockPos.STREAM_CODEC));

    public static final MenuType<HempPressScreenHandler> HEMP_PRESS =
            Registry.register(BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp_press"),
                    Services.MENUS.menuType(HempPressScreenHandler::new, BlockPos.STREAM_CODEC));

    public static void registerScreenHandlers() {
        Hempdustry.LOGGER.info("Registering Screen Handlers for " + Hempdustry.MOD_ID);
    }
}
