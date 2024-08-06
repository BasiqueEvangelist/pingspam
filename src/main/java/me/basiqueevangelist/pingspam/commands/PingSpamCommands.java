package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.commands.mail.*;
import me.basiqueevangelist.pingspam.logic.PingspamPermissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public class PingSpamCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        NotificationsCommand.register(dispatcher);
        AliasCommand.register(dispatcher);
        PingSoundCommand.register(dispatcher);
        PingIgnoreCommand.register(dispatcher);
        GroupCommand.register(dispatcher);
        ChatCommand.register(dispatcher);

        SendCommand.register(dispatcher);
        ListCommand.register(dispatcher);
        DeleteCommand.register(dispatcher);
        ClearCommand.register(dispatcher);

        dispatcher.register(
            literal("pingspam")
                .then(literal("reload")
                    .requires(PingspamPermissions::reload)
                    .executes(ctx -> {
                        PingSpam.CONFIG.load();

                        return 0;
                    })));
    }
}
