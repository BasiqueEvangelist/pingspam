package me.basiqueevangelist.pingspam.client;

import me.basiqueevangelist.pingspam.ConfigManager;
import me.basiqueevangelist.pingspam.client.config.PingSpamClientConfig;
import me.basiqueevangelist.pingspam.client.network.PingSpamClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PingSpamClient implements ClientModInitializer {
    public static final ConfigManager<PingSpamClientConfig> CONFIG = new ConfigManager<>(PingSpamClientConfig.class, new PingSpamClientConfig(), "pingspam-client.json5");

    @Override
    public void onInitializeClient() {
        PingSpamClientNetworking.register();
    }
}
