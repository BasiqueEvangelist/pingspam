package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public class PingSpamCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        NotificationsCommand.register(dispatcher);
        AliasCommand.register(dispatcher);
        PingSoundCommand.register(dispatcher);
        PingIgnoreCommand.register(dispatcher);
    }
}
