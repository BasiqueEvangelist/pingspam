package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class GroupChatLogic {
    private GroupChatLogic() {

    }

    public static void sendIn(ServerPlayerEntity player, String groupName, SignedMessage message) {
        // TODO: actually use secure chat.

        MinecraftServer server = player.getServer();
        var group = DataStore.getFor(server).get(PingSpam.GLOBAL_DATA).groups().get(groupName);
        var text = Text.literal("[")
            .append(Text.literal("@" + groupName)
                .formatted(Formatting.YELLOW))
            .append("] ")
            .append(player.getName())
            .append(": ")
            .append(message.getContent());

        PingLogic.ProcessedPing ping =
            PingLogic.processPings(server, message.getContent(), text, player.getUuid(), group.members()::contains);

        for (var member : group.members()) {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(member);
            if (online == null) continue;

            if (ping.pingSucceeded) {
                if (ping.pingedPlayers.contains(player.getUuid())) {
                    online.sendMessage(text.copy().formatted(Formatting.AQUA), false);
                } else if (ping.sender == player) {
                    online.sendMessage(text.copy().formatted(Formatting.GOLD), false);
                    ping.pingedPlayers.add(ping.sender.getUuid());
                }
            }

            if (!ping.pingedPlayers.contains(player.getUuid())) {
                online.sendMessage(text, false);
            }
        }
    }

    public static void changeChat(ServerPlayerEntity player, @Nullable String groupName) {
        MinecraftServer server = player.getServer();
        var group = DataStore.getFor(server).get(PingSpam.GLOBAL_DATA).groups().get(groupName);
        var playerData = DataStore.getFor(server).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        if (Objects.equals(playerData.currentChat(), groupName)) return;

        ServerNetworkLogic.sendServerAnnouncement(player, player.networkHandler.connection);
    }
}
