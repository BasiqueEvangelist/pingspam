package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

public class NotificationsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("notifications")
                .executes(NotificationsCommand::showNotifications)
        );
    }

    public static int showNotifications(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();

        List<Text> notifs = ((ServerPlayerEntityAccess) src.getPlayer()).pingspam$getPings();
        if (!notifs.isEmpty()) {
            src.sendFeedback(new LiteralText("You have " + notifs.size() + " unread message" + (notifs.size() != 1 ? "s" : "") + ":"), false);
            for (Text notif : notifs) {
                src.sendFeedback(notif, false);
            }
            notifs.clear();
            return 1;
        } else {
            src.sendFeedback(new LiteralText("You have no unread messages."), false);
            return 0;
        }
    }
}
