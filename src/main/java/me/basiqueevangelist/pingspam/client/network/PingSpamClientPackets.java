package me.basiqueevangelist.pingspam.client.network;

import me.basiqueevangelist.pingspam.client.PingSpamClient;
import me.basiqueevangelist.pingspam.network.AnnounceS2CPayload;
import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import me.basiqueevangelist.pingspam.network.PossibleNamesDiffS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

@Environment(EnvType.CLIENT)
public class PingSpamClientPackets {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(AnnounceS2CPayload.ID, (packet, ctx) -> {
            PingSpamClient.SERVER_DATA = new ServerData(packet);

//            ctx.responseSender().sendPacket(PingSpamPackets.ANNOUNCE, PacketByteBufs.empty());
        });

        ClientPlayNetworking.registerGlobalReceiver(PossibleNamesDiffS2CPacket.ID, (packet, ctx) -> {
            ServerData data = PingSpamClient.SERVER_DATA;

            if (data != null) {
                packet.removedNames().forEach(data.possibleNames()::remove);
                data.possibleNames().addAll(packet.addedNames());
            }
        });
    }
}
