package me.basiqueevangelist.pingspam.client;

import me.basiqueevangelist.pingspam.client.network.PingSpamClientPackets;
import net.fabricmc.api.ClientModInitializer;

public class PingSpamClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PingSpamClientPackets.register();
    }
}
