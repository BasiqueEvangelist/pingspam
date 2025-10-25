package me.basiqueevangelist.pingspam.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PingSpamPackets {
    public static void register() {
        PayloadTypeRegistry.playS2C().register(AnnounceS2CPayload.ID, AnnounceS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PossibleNamesDiffS2CPacket.ID, PossibleNamesDiffS2CPacket.CODEC);
    }
}
