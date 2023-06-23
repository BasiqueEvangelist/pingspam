package me.basiqueevangelist.pingspam.client.network;

import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import me.basiqueevangelist.pingspam.utils.CaseInsensitiveUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.Set;

@Environment(EnvType.CLIENT)
public class ServerData {
    private static final long MAX_TIME_SINCE_REQUEST = 1000000000L * 30;
    private long lastRequestTime = 0;

    private boolean canPingPlayers;
    private final Set<String> possibleNames = CaseInsensitiveUtil.treeSetIgnoringCase();
    private int version = 0;

    public boolean canPingPlayers() {
        return canPingPlayers;
    }

    public Set<String> possibleNames() {
        return possibleNames;
    }

    public void setPermissions(boolean canPingEveryone, boolean canPingOnline, boolean canPingOffline, boolean canPingPlayers) {
        this.canPingPlayers = canPingPlayers;

        if (canPingEveryone) {
            possibleNames.add("everyone");
        } else {
            possibleNames.remove("everyone");
        }

        if (canPingOnline) {
            possibleNames.add("online");
        } else {
            possibleNames.remove("online");
        }

        if (canPingOffline) {
            possibleNames.add("offline");
        } else {
            possibleNames.remove("offline");
        }
    }

    public int version() {
        return version;
    }

    public void version(int version) {
        this.version = version;
    }

    public void refreshPermissionsIfNeeded() {
        if ((System.nanoTime() - lastRequestTime) > MAX_TIME_SINCE_REQUEST) {
            ClientPlayNetworking.send(PingSpamPackets.PULL_PERMISSIONS, PacketByteBufs.empty());
            lastRequestTime = System.nanoTime();
        }
    }
}
