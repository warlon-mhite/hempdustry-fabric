package com.warlonmhite.hempdustry.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionLevel;

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
        Services.EVENTS.onRegisterCommands(dispatcher -> dispatcher.register(build()));
        Hempdustry.LOGGER.info("Registering Commands for " + Hempdustry.MOD_ID);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(Hempdustry.MOD_ID)
                // Permission is a PermissionLevel check since 1.21.10 rather than a bare int.
                .requires(Commands.hasPermission(
                        new PermissionCheck.Require(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))))
                .then(Commands.literal("reload").executes(context -> {
                    if (!HempdustryConfig.load()) {
                        // The file could not be read and was left alone; the settings already running
                        // stay. Say so, or a typo looks exactly like a reload that took.
                        context.getSource().sendFailure(Component.translatable("commands.hempdustry.reload.failed"));
                        return 0;
                    }
                    // Sent as a broadcast-to-ops message: a balance change is something the other
                    // operators online want to know happened, the way /gamerule announces itself.
                    context.getSource().sendSuccess(
                            () -> Component.translatable("commands.hempdustry.reload"), true);
                    return 1;
                }));
    }
}
