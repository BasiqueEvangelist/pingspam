package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

        List<Text> notifs = PlayerUtils.getUnreadPingsFor(src.getPlayer());
        if (!notifs.isEmpty()) {
            MutableText response = new LiteralText("You have " + notifs.size() + " unread message" + (notifs.size() != 1 ? "s" : "") + ":")
                .formatted(Formatting.GREEN);
            for (Text notif : notifs) {
                response.append(new LiteralText("\n- ")
                    .formatted(Formatting.WHITE)
                    .append(notif));
            }
            src.sendFeedback(response, false);
            notifs.clear();
            return 1;
        } else {
            src.sendFeedback(new LiteralText("You have no unread messages.")
                .formatted(Formatting.GREEN), false);
            return 0;
        }
    }
}
