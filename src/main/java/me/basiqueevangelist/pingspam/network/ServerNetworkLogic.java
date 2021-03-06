package me.basiqueevangelist.pingspam.network;

import me.basiqueevangelist.pingspam.utils.NameLogic;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public final class ServerNetworkLogic {
    private ServerNetworkLogic() {

    }

    public static void sendServerAnnouncement(ServerPlayerEntity player, ClientConnection conn) {
        if (!ServerPlayNetworking.canSend(player, PingSpamPackets.ANNOUNCE)) return;

        PlayerManager manager = player.server.getPlayerManager();
        PacketByteBuf newBuf = PacketByteBufs.create();

        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.everyone", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.online", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.offline", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.player", true));

        List<String> possibleNames = NameLogic.listValidNames(manager);
        newBuf.writeVarInt(possibleNames.size());
        for (String possibleName : possibleNames) {
            newBuf.writeString(possibleName);
        }

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
            if (ServerPlayNetworking.canSend(player, channel))
                player.networkHandler.sendPacket(packet);
        }
    }
}
