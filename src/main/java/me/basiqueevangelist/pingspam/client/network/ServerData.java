package me.basiqueevangelist.pingspam.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ServerData {
    public boolean canPingEveryone;
    public boolean canPingOnline;
    public boolean canPingOffline;
    public boolean canPingPlayers;
    public List<String> possibleNames = new ArrayList<>();
}
