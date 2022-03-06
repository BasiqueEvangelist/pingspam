package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.pingspam.PingSpam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NameLogic {
    private NameLogic() {

    }

    public static boolean isValidName(MinecraftServer server, String name, boolean ignoreGroups) {
        if (name.equals("everyone") || name.equals("online") || name.equals("offline"))
            return true;

        for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
            if (onlinePlayer.getGameProfile().getName().equalsIgnoreCase(name))
                return true;
        }

        for (PlayerDataEntry entry : DataStore.getFor(server).players()) {
            if (NameUtil.getNameFromUUID(entry.playerId()).equalsIgnoreCase(name))
                return true;

            if (entry.get(PingSpam.PLAYER_DATA).aliases().contains(name))
                return true;
        }

        if (!ignoreGroups) {
            if (DataStore.getFor(server).get(PingSpam.GLOBAL_DATA).groups().containsKey(name)) {
                return true;
            }
        }

        return false;
    }

    public static Set<String> listValidNames(MinecraftServer server) {
        Set<String> possibleNames = CaseInsensitiveUtil.setIgnoringCase();

        for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
            String playerName = otherPlayer.getGameProfile().getName();
            possibleNames.add(playerName);
        }

        for (PlayerDataEntry entry : DataStore.getFor(server).players()) {
            String name = NameUtil.getNameFromUUIDOrNull(entry.playerId());

            if (name != null)
                possibleNames.add(name);

            possibleNames.addAll(entry.get(PingSpam.PLAYER_DATA).aliases());
        }

        possibleNames.addAll(DataStore.getFor(server).get(PingSpam.GLOBAL_DATA).groups().keySet());

        return possibleNames;
    }
}
