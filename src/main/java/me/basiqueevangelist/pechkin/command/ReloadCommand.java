package me.basiqueevangelist.pechkin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.basiqueevangelist.pechkin.Pechkin;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public final class ReloadCommand {
    private ReloadCommand() {

    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mail")
            .then(literal("reload")
                .requires(Permissions.require("pechkin.reload", 2))
                .executes(ReloadCommand::reload)));
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        Pechkin.CONFIG.load();

        return 0;
    }
}
