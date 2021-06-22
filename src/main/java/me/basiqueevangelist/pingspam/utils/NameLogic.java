package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.basiqueevangelist.nevseti.nbt.CompoundTagView;
import me.basiqueevangelist.nevseti.nbt.ListTagView;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NameLogic {
    private NameLogic() {

    }

    public static boolean isValidName(PlayerManager manager, String name, boolean ignoreGroups) {
        for (ServerPlayerEntity onlinePlayer : manager.getPlayerList()) {
            if (onlinePlayer.getGameProfile().getName().equalsIgnoreCase(name))
                return true;

            for (String otherAlias : PlayerUtils.getAliasesOf(onlinePlayer)) {
                if (otherAlias.equalsIgnoreCase(name))
                    return true;
            }

            if (!ignoreGroups)
                for (String group : PlayerUtils.getPingGroupsOf(onlinePlayer)) {
                    if (group.equalsIgnoreCase(name))
                        return true;
                }
        }

        for (Map.Entry<UUID, CompoundTagView> offlineTag : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("SavedUsername") && offlineTag.getValue().getString("SavedUsername").equalsIgnoreCase(name)) {
                return true;
            }

            if (offlineTag.getValue().contains("Shortnames")) {
                ListTagView aliasesTag = offlineTag.getValue().getList("Shortnames", 8);
                for (int i = 0; i < aliasesTag.size(); i++) {
                    if (aliasesTag.getString(i).equalsIgnoreCase(name))
                        return true;
                }
            }

            if (!ignoreGroups)
                if (offlineTag.getValue().contains("PingGroups")) {
                    ListTagView pingGroupsTag = offlineTag.getValue().getList("PingGroups", 8);
                    for (int i = 0; i < pingGroupsTag.size(); i++) {
                        if (pingGroupsTag.getString(i).equalsIgnoreCase(name))
                            return true;
                    }
                }
        }

        return false;
    }

    public static List<String> listValidNames(PlayerManager manager) {
        List<String> possibleNames = new ArrayList<>();
        for (ServerPlayerEntity otherPlayer : manager.getPlayerList()) {
            String playerName = otherPlayer.getGameProfile().getName();
            if (possibleNames.stream().noneMatch(x -> x.equalsIgnoreCase(playerName)))
                possibleNames.add(playerName);

            for (String alias : PlayerUtils.getAliasesOf(otherPlayer)) {
                if (possibleNames.stream().noneMatch(x -> x.equalsIgnoreCase(alias)))
                    possibleNames.add(alias);
            }

            for (String group : PlayerUtils.getPingGroupsOf(otherPlayer)) {
                if (possibleNames.stream().noneMatch(x -> x.equalsIgnoreCase(group)))
                    possibleNames.add(group);
            }
        }
        for (Map.Entry<UUID, CompoundTagView> offlinePlayerTag : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlinePlayerTag.getKey()) != null)
                continue;

            if (offlinePlayerTag.getValue().contains("SavedUsername")) {
                String offlineUsername = offlinePlayerTag.getValue().getString("SavedUsername");
                if (possibleNames.stream().noneMatch(x -> x.equalsIgnoreCase(offlineUsername)))
                    possibleNames.add(offlineUsername);
            }
            if (offlinePlayerTag.getValue().contains("Shortnames")) {
                ListTagView aliasesTag = offlinePlayerTag.getValue().getList("Shortnames", 8);
                for (int i = 0; i < aliasesTag.size(); i++) {
                    String alias = aliasesTag.getString(i);
                    if (possibleNames.stream().noneMatch(x -> x.equalsIgnoreCase(alias)))
                        possibleNames.add(alias);
                }
            }
            if (offlinePlayerTag.getValue().contains("PingGroups")) {
                ListTagView pingGroupsTag = offlinePlayerTag.getValue().getList("PingGroups", 8);
                for (int i = 0; i < pingGroupsTag.size(); i++) {
                    String pingGroup = pingGroupsTag.getString(i);
                    if (possibleNames.stream().noneMatch(x -> x.equalsIgnoreCase(pingGroup)))
                        possibleNames.add(pingGroup);
                }
            }
        }
        return possibleNames;
    }
}
