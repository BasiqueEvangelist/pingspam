package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.basiqueevangelist.pingspam.PingSpam;
import me.lucko.fabric.api.permissions.v0.Permissions;
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

        dispatcher.register(
            literal("pingspam")
                .then(literal("reload")
                    .requires(x -> Permissions.check(x, "pingspam.reload", 2))
                    .executes(ctx -> {
                        PingSpam.CONFIG.load();

                        return 0;
                    })));
    }
}
