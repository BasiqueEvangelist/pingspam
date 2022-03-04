package me.basiqueevangelist.pingspam.client.network;

import me.basiqueevangelist.pingspam.client.PingSpamClient;
import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

@Environment(EnvType.CLIENT)
public class PingSpamClientPackets {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PingSpamPackets.ANNOUNCE, (client, handler, buf, responseSender) -> {
            ServerData data = new ServerData();

            boolean canPingEveryone = buf.readBoolean();
            boolean canPingOnline = buf.readBoolean();
            boolean canPingOffline = buf.readBoolean();
            boolean canPingPlayers = buf.readBoolean();
            data.setPermissions(canPingEveryone, canPingOnline, canPingOffline, canPingPlayers);

            int namesCount = buf.readVarInt();
            for (int i = 0; i < namesCount; i++) {
                data.possibleNames().add(buf.readString());
            }

            PingSpamClient.SERVER_DATA = data;

            responseSender.sendPacket(PingSpamPackets.ANNOUNCE, PacketByteBufs.empty());
        });

        ClientPlayNetworking.registerGlobalReceiver(PingSpamPackets.PULL_PERMISSIONS, (client, handler, buf, responseSender) -> {
            ServerData data = PingSpamClient.SERVER_DATA;

            if (data != null) {
                boolean canPingEveryone = buf.readBoolean();
                boolean canPingOnline = buf.readBoolean();
                boolean canPingOffline = buf.readBoolean();
                boolean canPingPlayers = buf.readBoolean();

                data.setPermissions(canPingEveryone, canPingOnline, canPingOffline, canPingPlayers);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(PingSpamPackets.POSSIBLE_NAMES_DIFF, (client, handler, buf, responseSender) -> {
            ServerData data = PingSpamClient.SERVER_DATA;

            if (data != null) {
                int addedNamesCount = buf.readVarInt();
                for (int i = 0; i < addedNamesCount; i++) {
                    String name = buf.readString();
                    data.possibleNames().add(name);
                }

                int removedNamesCount = buf.readVarInt();
                for (int i = 0; i < removedNamesCount; i++) {
                    data.possibleNames().remove(buf.readString());
                }
            }
        });
    }
}
