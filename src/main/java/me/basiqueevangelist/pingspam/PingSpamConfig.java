package me.basiqueevangelist.pingspam;

import blue.endless.jankson.Comment;

public class PingSpamConfig {
    @Comment("Send errors to players if a message contains invalid pings.")
    public boolean sendPingErrors = true;

    @Comment("Show amount of unread messages in actionbar.")
    public boolean showUnreadMessagesInActionbar = true;

    @Comment("Process pings from messages with nil UUIDs. Disabling this will disallow pings from command blocks and the server console, but will also disallow random unprivileged discord users from pinging @everyone with a discord link.")
    public boolean processPingsFromUnknownPlayers = true;
}
