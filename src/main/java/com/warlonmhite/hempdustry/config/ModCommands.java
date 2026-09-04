package com.warlonmhite.hempdustry.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * {@code /hempdustry reload} — re-reads {@code config/hempdustry.json} without a restart.
 *
 * <p>Permission level 2, the same bar vanilla puts on {@code /reload} and {@code /gamerule}: this
 * changes how the mod behaves for everyone on the server.
 *
 * <p><b>It does not reload strains</b>, and that is deliberate rather than an omission. Strains are a
 * datapack registry, so vanilla's own {@code /reload} is what re-reads them — telling an admin to run
 * two different reload commands for two halves of the same tuning session would be worse than
 * telling them which one does what. The chat reply says so.
 */
public class ModCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(build()));
        Hempdustry.LOGGER.info("Registering Commands for " + Hempdustry.MOD_ID);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal(Hempdustry.MOD_ID)
                // Permission is a PermissionLevel check since 1.21.10 rather than a bare int.
                .requires(CommandManager.requirePermissionLevel(
                        new PermissionCheck.Require(new Permission.Level(PermissionLevel.GAMEMASTERS))))
                .then(CommandManager.literal("reload").executes(context -> {
                    HempdustryConfig.load();
                    // Sent as a broadcast-to-ops message: a balance change is something the other
                    // operators online want to know happened, the way /gamerule announces itself.
                    context.getSource().sendFeedback(
                            () -> Text.translatable("commands.hempdustry.reload"), true);
                    return 1;
                }));
    }
}
