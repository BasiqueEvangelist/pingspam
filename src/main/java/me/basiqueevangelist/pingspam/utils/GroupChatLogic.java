package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class GroupChatLogic {
    private GroupChatLogic() {

    }

    public static void sendIn(ServerPlayerEntity player, String groupName, SignedMessage message) {
        // TODO: actually use secure chat.

        MinecraftServer server = player.getServer();
        var group = DataStore.getFor(server).get(PingSpam.GLOBAL_DATA).groups().get(groupName);
        var text = Text.literal("[")
            .append(Text.literal("@" + groupName)
                .formatted(Formatting.AQUA))
            .append("] ")
            .append(player.getName())
            .append(": ")
            .append(message.getContent());

        for (var member : group.members()) {
            PingLogic.sendNotification(server, member, text);

            ServerPlayerEntity online = server.getPlayerManager().getPlayer(member);
            if (online != null)
                online.sendMessage(text);
        }
    }
}
