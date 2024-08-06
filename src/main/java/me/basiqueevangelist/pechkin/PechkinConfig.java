package me.basiqueevangelist.pechkin;

import blue.endless.jankson.Comment;

public class PechkinConfig {
    @Comment("The maximum amount of messages a player's inbox can have.")
    public int maxInboxMessages = 100;

    @Comment("The amount of players that will be stored in the player's correspondents list. Used for command suggestions.")
    public int maxCorrespondents = 10;

    @Comment("The maximum amount of time debt a player's leaky bucket can have.")
    public int maxTimeDebt = 120;

    @Comment("The cost of sending a single message.")
    public int sendCost = 30;
}
