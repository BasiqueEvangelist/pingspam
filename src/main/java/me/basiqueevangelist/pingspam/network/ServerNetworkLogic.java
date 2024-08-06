package me.basiqueevangelist.pingspam.network;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.NameLogic;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Set;

public final class ServerNetworkLogic {
    public static final int ANNOUNCEMENT_VERSION = 1;

    private ServerNetworkLogic() {

    }

    public static void sendServerAnnouncement(ServerPlayerEntity player, ClientConnection conn) {
        DataStore store = DataStore.getFor(player.getWorld().getServer());
        var data = store.getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        PacketByteBuf newBuf = PacketByteBufs.create();

        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.everyone", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.online", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.offline", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.player", true));

        Set<String> possibleNames;
        var group = data.currentChat() == null ? null : store.get(PingSpam.GLOBAL_DATA).groups().get(data.currentChat());

        if (group != null) {
            possibleNames = NameLogic.listValidNames(player.server, group.members()::contains, false);
        } else {
            possibleNames = NameLogic.listValidNames(player.server, uuid -> true, true);
        }

        newBuf.writeCollection(possibleNames, PacketByteBuf::writeString);

        newBuf.writeVarInt(ANNOUNCEMENT_VERSION);

        conn.send(ServerPlayNetworking.createS2CPacket(PingSpamPackets.ANNOUNCE, newBuf));
    }

    public static void removePossibleName(PlayerManager manager, String possibleName) {
        PacketByteBuf diffBuf = PacketByteBufs.create();
        diffBuf.writeVarInt(0);
        diffBuf.writeVarInt(1);
        diffBuf.writeString(possibleName);
        sendToAll(manager, PingSpamPackets.POSSIBLE_NAMES_DIFF, diffBuf);
    }

    public static void addPossibleName(PlayerManager manager, String possibleName) {
        PacketByteBuf diffBuf = PacketByteBufs.create();
        diffBuf.writeVarInt(1);
        diffBuf.writeString(possibleName);
        diffBuf.writeVarInt(0);
        sendToAll(manager, PingSpamPackets.POSSIBLE_NAMES_DIFF, diffBuf);
    }

    public static void sendToAll(PlayerManager manager, Identifier channel, PacketByteBuf buf) {
        Packet<?> packet = ServerPlayNetworking.createS2CPacket(channel, buf);
        for (ServerPlayerEntity player : manager.getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, channel) || PingSpam.CONFIG.getConfig().ignoreCanSend)
                player.networkHandler.sendPacket(packet);
        }
    }
}
