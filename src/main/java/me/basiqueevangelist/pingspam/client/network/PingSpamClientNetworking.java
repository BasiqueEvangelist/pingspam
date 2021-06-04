package me.basiqueevangelist.pingspam.client.network;

import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PingSpamClientNetworking {
    private static ServerData currentServerData = null;
    private static final long MAX_TIME_SINCE_REQUEST = 1000000000L * 10;
    private static long lastPermissionsRequest = 0;

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

            currentServerData = data;

            responseSender.sendPacket(PingSpamPackets.ANNOUNCE, PacketByteBufs.empty());
        });

        ClientPlayNetworking.registerGlobalReceiver(PingSpamPackets.PULL_PERMISSIONS, (client, handler, buf, responseSender) -> {
            currentServerData.canPingEveryone = buf.readBoolean();
            currentServerData.canPingOnline = buf.readBoolean();
            currentServerData.canPingOffline = buf.readBoolean();
            currentServerData.canPingPlayers = buf.readBoolean();
        });

        ClientPlayNetworking.registerGlobalReceiver(PingSpamPackets.POSSIBLE_NAMES_DIFF, (client, handler, buf, responseSender) -> {
            if (currentServerData != null) {
                int addedNamesCount = buf.readVarInt();
                for (int i = 0; i < addedNamesCount; i++) {
                    String name = buf.readString();
                    if (!currentServerData.possibleNames.contains(name))
                        currentServerData.possibleNames.add(name);
                }

                int removedNamesCount = buf.readVarInt();
                for (int i = 0; i < removedNamesCount; i++) {
                    currentServerData.possibleNames.remove(buf.readString());
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentServerData = null;
            lastPermissionsRequest = 0;
        });
    }

    public static void requestServerData() {
        if (currentServerData != null && (System.nanoTime() - lastPermissionsRequest) > MAX_TIME_SINCE_REQUEST) {
            ClientPlayNetworking.send(PingSpamPackets.PULL_PERMISSIONS, PacketByteBufs.empty());
            lastPermissionsRequest = System.nanoTime();
        }
    }

    public static @Nullable ServerData getServerData() {
        return currentServerData;
    }
}
