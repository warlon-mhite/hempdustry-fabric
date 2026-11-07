package com.warlonmhite.hempdustry.screen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Menus;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreenHandler;
import com.warlonmhite.hempdustry.screen.custom.HempPressScreenHandler;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    /**
     * Extended rather than plain so the block's position rides along to the client, which is what
     * lets the screen resolve the block entity it belongs to.
     */
    public static final MenuType<DecarboxylatorScreenHandler> DECARBOXYLATOR =
            Registry.register(Registry.MENU,
                    new ResourceLocation(Hempdustry.MOD_ID, "decarboxylator"),
                    Services.MENUS.menuType(DecarboxylatorScreenHandler::new, Menus.Data.BLOCK_POS));

    public static final MenuType<InfuserScreenHandler> INFUSER =
            Registry.register(Registry.MENU,
                    new ResourceLocation(Hempdustry.MOD_ID, "infuser"),
                    Services.MENUS.menuType(InfuserScreenHandler::new, Menus.Data.BLOCK_POS));

    public static final MenuType<HempPressScreenHandler> HEMP_PRESS =
            Registry.register(Registry.MENU,
                    new ResourceLocation(Hempdustry.MOD_ID, "hemp_press"),
                    Services.MENUS.menuType(HempPressScreenHandler::new, Menus.Data.BLOCK_POS));

    public static void registerScreenHandlers() {
        Hempdustry.LOGGER.info("Registering Screen Handlers for " + Hempdustry.MOD_ID);
    }
}
