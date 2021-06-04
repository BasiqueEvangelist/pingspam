package me.basiqueevangelist.pingspam.client;

import me.basiqueevangelist.pingspam.client.network.PingSpamClientNetworking;
import me.basiqueevangelist.pingspam.client.network.ServerData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PingSpamClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PingSpamClientNetworking.register();
    }
}
