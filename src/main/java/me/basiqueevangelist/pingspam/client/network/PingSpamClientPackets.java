package me.basiqueevangelist.pingspam.client.network;

import me.basiqueevangelist.pingspam.access.ClientPlayNetworkHandlerAccess;
import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public class PingSpamClientPackets {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PingSpamPackets.ANNOUNCE, (client, handler, buf, responseSender) -> {
            ServerData data = new ServerData();
            data.canPingEveryone = buf.readBoolean();
            data.canPingOnline = buf.readBoolean();
            data.canPingOffline = buf.readBoolean();
            data.canPingPlayers = buf.readBoolean();
            int suggestedNamesCount = buf.readVarInt();
            for (int i = 0; i < suggestedNamesCount; i++) {
                String possibleName = buf.readString();
                if (!data.possibleNames.contains(possibleName))
                    data.possibleNames.add(possibleName);
            }

            ((ClientPlayNetworkHandlerAccess) handler).pingspam$setServerData(data);

            responseSender.sendPacket(PingSpamPackets.ANNOUNCE, PacketByteBufs.empty());
        });

        ClientPlayNetworking.registerGlobalReceiver(PingSpamPackets.PULL_PERMISSIONS, (client, handler, buf, responseSender) -> {
            ServerData data = ((ClientPlayNetworkHandlerAccess) handler).pingspam$getServerData();
            data.canPingEveryone = buf.readBoolean();
            data.canPingOnline = buf.readBoolean();
            data.canPingOffline = buf.readBoolean();
            data.canPingPlayers = buf.readBoolean();
        });

        ClientPlayNetworking.registerGlobalReceiver(PingSpamPackets.POSSIBLE_NAMES_DIFF, (client, handler, buf, responseSender) -> {
            ServerData data = ((ClientPlayNetworkHandlerAccess) handler).pingspam$getServerData();
            if (data != null) {
                int addedNamesCount = buf.readVarInt();
                for (int i = 0; i < addedNamesCount; i++) {
                    String name = buf.readString();
                    if (!data.possibleNames.contains(name))
                        data.possibleNames.add(name);
                }

                int removedNamesCount = buf.readVarInt();
                for (int i = 0; i < removedNamesCount; i++) {
                    data.possibleNames.remove(buf.readString());
                }
            }
        });
    }
}
