package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import net.minecraft.server.command.ServerCommandSource;
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

        dispatcher.register(
            literal("pingspam")
                .executes(NotificationsCommand::showNotifications)
        );
    }

    public static int showNotifications(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(src.getPlayerOrThrow().getUuid(), PingSpam.PLAYER_DATA);

        if (!data.unreadPings().isEmpty()) {
            MutableText response = Text.literal("You have " + data.unreadPings().size() + " unread message" + (data.unreadPings().size() != 1 ? "s" : "") + ":")
                .formatted(Formatting.GREEN);

            for (Text notif : data.unreadPings()) {
                response.append(Text.literal("\n- ")
                    .formatted(Formatting.WHITE)
                    .append(notif));
            }

            src.sendFeedback(response, false);
            data.unreadPings().clear();

            return 1;
        } else {
            src.sendFeedback(Text.literal("You have no unread messages.")
                .formatted(Formatting.GREEN), false);

            return 0;
        }
    }
}
