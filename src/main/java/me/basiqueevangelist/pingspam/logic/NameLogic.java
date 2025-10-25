package me.basiqueevangelist.pingspam.logic;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.CaseInsensitiveUtil;
import me.basiqueevangelist.pingspam.utils.NameUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class NameLogic {
    private NameLogic() {

    }

    public static boolean isValidName(MinecraftServer server, String name, boolean ignoreGroups) {
        if (name.equals("everyone") || name.equals("online") || name.equals("offline"))
            return true;

        for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
            if (onlinePlayer.getGameProfile().name().equalsIgnoreCase(name))
                return true;
        }

        for (PlayerDataEntry entry : DataStore.getFor(server).players()) {
            if (NameUtil.getNameFromUUID(entry.playerId()).equalsIgnoreCase(name))
                return true;

            if (entry.get(PingSpam.PLAYER_DATA).aliases().contains(name))
                return true;
        }

        if (!ignoreGroups) {
            var group = DataStore.getFor(server).get(PingSpam.GLOBAL_DATA).groups().get(name);

            if (group != null && group.isPingable()) {
                return true;
            }
        }

        return false;
    }

    public static Set<String> listValidNames(MinecraftServer server, Predicate<UUID> playerPredicate,
                                             boolean includeGroups) {
        Set<String> possibleNames = CaseInsensitiveUtil.setIgnoringCase();

        for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
            if (!playerPredicate.test(otherPlayer.getUuid())) continue;

            String playerName = otherPlayer.getGameProfile().name();
            possibleNames.add(playerName);
        }

        for (PlayerDataEntry entry : DataStore.getFor(server).players()) {
            if (!playerPredicate.test(entry.playerId())) continue;

            String name = NameUtil.getNameFromUUIDOrNull(entry.playerId());

            if (name != null)
                possibleNames.add(name);

            possibleNames.addAll(entry.get(PingSpam.PLAYER_DATA).aliases());
        }

        if (includeGroups) {
            for (var group : DataStore.getFor(server).get(PingSpam.GLOBAL_DATA).groups().values()) {
                if (!group.isPingable()) continue;

                possibleNames.add(group.name());
            }
        }

        return possibleNames;
    }
}
