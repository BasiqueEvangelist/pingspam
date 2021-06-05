package me.basiqueevangelist.pingspam.client.config;

import me.basiqueevangelist.pingspam.client.PingSpamClient;
import me.basiqueevangelist.pingspam.client.network.PingSpamClientNetworking;
import me.shedaniel.clothconfiglite.api.ConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public final class ConfigScreenBuilder {
    private ConfigScreenBuilder() {

    }

    private static final Text SCREEN_TITLE = new TranslatableText("pingspam.config.client.title");

    public static Screen buildConfigScreen(Screen parent) {
        ConfigScreen screen = ConfigScreen.create(SCREEN_TITLE, parent);
        screen.add(new TranslatableText("pingspam.config.client.serverIntegration"), PingSpamClient.CONFIG.getConfig().serverIntegration, () -> true, val -> {
            PingSpamClient.CONFIG.getConfig().serverIntegration = (boolean) val;
            if (!PingSpamClient.CONFIG.getConfig().serverIntegration)
                PingSpamClientNetworking.disable();
            PingSpamClient.CONFIG.save();
        });
        screen.add(new TranslatableText("pingspam.config.client.localMentionScanning"), PingSpamClient.CONFIG.getConfig().localMentionScanning, () -> true, val -> {
            PingSpamClient.CONFIG.getConfig().localMentionScanning = (boolean) val;
            PingSpamClient.CONFIG.save();
        });
        screen.add(new TranslatableText("pingspam.config.client.alwaysScanMentions"), PingSpamClient.CONFIG.getConfig().alwaysScanMentions, () -> true, val -> {
            PingSpamClient.CONFIG.getConfig().alwaysScanMentions = (boolean) val;
            PingSpamClient.CONFIG.save();
        });
        return screen.get();
    }
}
