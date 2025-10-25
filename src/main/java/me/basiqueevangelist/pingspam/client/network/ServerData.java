package me.basiqueevangelist.pingspam.client.network;

import me.basiqueevangelist.pingspam.network.AnnounceS2CPayload;
import me.basiqueevangelist.pingspam.utils.CaseInsensitiveUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Set;

@Environment(EnvType.CLIENT)
public class ServerData {
    private boolean canPingPlayers;
    private final Set<String> possibleNames = CaseInsensitiveUtil.treeSetIgnoringCase();

    public ServerData(AnnounceS2CPayload payload) {
        setPermissions(payload.canPingEveryone(), payload.canPingOnline(), payload.canPingOffline(), payload.canPingPlayers());
        possibleNames.addAll(payload.possibleNames());
    }

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
}
