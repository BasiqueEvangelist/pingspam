package me.basiqueevangelist.pingspam;

import blue.endless.jankson.Comment;

public class PingSpamConfig {
    @Comment("Send errors to players if a message contains invalid pings.")
    public boolean sendPingErrors = true;

    @Comment("Show amount of unread messages in actionbar.")
    public boolean showUnreadMessagesInActionbar = true;

    @Comment("Process pings from messages with nil UUIDs. Disabling this will disallow pings from command blocks and the server console, but will also disallow random unprivileged discord users from pinging @everyone with a discord link.")
    public boolean processPingsFromUnknownPlayers = true;

    @Comment("""
        Makes Pingspam send all important packets to all clients, regardless of whether that client registered Pingspam
        channels as receivable.
        Can be important if your proxy somehow blocks these registrations.
        """)
    public boolean ignoreCanSend = false;

    public MailConfig mail = new MailConfig();

    public static class MailConfig {
        @Comment("The maximum amount of messages a player's inbox can have.")
        public int maxInboxMessages = 100;

        @Comment("The amount of players that will be stored in the player's correspondents list. Used for command suggestions.")
        public int maxCorrespondents = 10;

        @Comment("The maximum amount of time debt a player's leaky bucket can have.")
        public int maxTimeDebt = 120;

        @Comment("The cost of sending a single message.")
        public int sendCost = 30;
    }
}
