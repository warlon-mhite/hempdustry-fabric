package com.warlonmhite.hempdustry.screen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {
    /**
     * Extended rather than plain so the block's position rides along to the client, which is what
     * lets the screen resolve the block entity it belongs to.
     */
    public static final ScreenHandlerType<DecarboxylatorScreenHandler> DECARBOXYLATOR =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(Hempdustry.MOD_ID, "decarboxylator"),
                    new ExtendedScreenHandlerType<>(DecarboxylatorScreenHandler::new, BlockPos.PACKET_CODEC));

    public static void registerScreenHandlers() {
        Hempdustry.LOGGER.info("Registering Screen Handlers for " + Hempdustry.MOD_ID);
    }
}
