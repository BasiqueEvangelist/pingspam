package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.basiqueevangelist.pingspam.data.PingspamPersistentState;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        PingspamPlayerData data = PingspamPersistentState.getFrom(ctx.getSource().getServer()).getFor(src.getPlayer().getUuid());

        if (!data.unreadPings().isEmpty()) {
            MutableText response = new LiteralText("You have " + data.unreadPings().size() + " unread message" + (data.unreadPings().size() != 1 ? "s" : "") + ":")
                .formatted(Formatting.GREEN);

            for (Text notif : data.unreadPings()) {
                response.append(new LiteralText("\n- ")
                    .formatted(Formatting.WHITE)
                    .append(notif));
            }

            src.sendFeedback(response, false);
            data.unreadPings().clear();

            return 1;
        } else {
            src.sendFeedback(new LiteralText("You have no unread messages.")
                .formatted(Formatting.GREEN), false);

            return 0;
        }
    }
}
