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
 * <p><b>It does not reload strains, and nothing can short of a restart.</b> Strains are a dynamic
 * registry, which the server reads once, when the world opens; vanilla's {@code /reload} keeps the
 * registries it has and only re-reads their tags. Until 2.0.3 the chat reply sent admins to
 * {@code /reload} for strains, which did nothing.
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
                    if (!HempdustryConfig.load()) {
                        // The file could not be read and was left alone; the settings already running
                        // stay. Say so, or a typo looks exactly like a reload that took.
                        context.getSource().sendError(Text.translatable("commands.hempdustry.reload.failed"));
                        return 0;
                    }
                    // Sent as a broadcast-to-ops message: a balance change is something the other
                    // operators online want to know happened, the way /gamerule announces itself.
                    context.getSource().sendFeedback(
                            () -> Text.translatable("commands.hempdustry.reload"), true);
                    return 1;
                }));
    }
}
