package me.basiqueevangelist.pingspam.client;

import me.basiqueevangelist.pingspam.client.network.PingSpamClientPackets;
import me.basiqueevangelist.pingspam.client.network.ServerData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

@Environment(EnvType.CLIENT)
public class PingSpamClient implements ClientModInitializer {
    public static ServerData SERVER_DATA;

    @Override
    public void onInitializeClient() {
        PingSpamClientPackets.register();

        ClientPlayConnectionEvents.DISCONNECT.register(
            (handler, client) -> SERVER_DATA = null);

        ClientLoginConnectionEvents.DISCONNECT.register(
            (handler, client) -> SERVER_DATA = null);
    }
}
