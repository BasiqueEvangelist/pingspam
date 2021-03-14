package me.basiqueevangelist.pingspam;

import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import me.sargunvohra.mcmods.autoconfig1u.shadowed.blue.endless.jankson.Comment;

@Config(name = "pingspam")
public class PingSpamConfig implements ConfigData {
    @Comment("Send errors to players if a message contains invalid pings.")
    public boolean sendPingErrors = true;

    @Comment("Show amount of unread messages in actionbar.")
    public boolean showUnreadMessagesInActionbar = true;

    @Comment("Process pings from messages with nil UUIDs. Disabling this will disallow pings from command blocks and the server console, but will also disallow random unprivileged discord users from pinging @everyone with a discord link.")
    public boolean processPingsFromUnknownPlayers = true;
}
