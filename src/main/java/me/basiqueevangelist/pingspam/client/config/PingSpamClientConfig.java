package me.basiqueevangelist.pingspam.client.config;

import blue.endless.jankson.Comment;

public class PingSpamClientConfig {
    @Comment("Enable/disable integration with the Pingspam server mod.")
    public boolean serverIntegration = true;

    @Comment("Scan messages for mentions on the client")
    public boolean localMentionScanning = true;

    @Comment("Scan messages for mentions even if the server has Pingspam installed")
    public boolean alwaysScanMentions = true;
}
