package me.basiqueevangelist.pingspam.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
            if (onlinePlayer.getGameProfile().getName().equals(name))
                return true;

            for (String otherAlias : PlayerUtils.getAliasesOf(onlinePlayer)) {
                if (otherAlias.equals(name))
                    return true;
            }

            if (!ignoreGroups)
                for (String group : PlayerUtils.getPingGroupsOf(onlinePlayer)) {
                    if (group.equals(name))
                        return true;
                }
        }

        for (Map.Entry<UUID, CompoundTag> offlineTag : OfflinePlayerCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("SavedUsername") && offlineTag.getValue().getString("SavedUsername").equals(name)) {
                return true;
            }

            if (offlineTag.getValue().contains("Shortnames")) {
                ListTag aliasesTag = offlineTag.getValue().getList("Shortnames", 8);
                for (Tag aliasTag : aliasesTag) {
                    if (aliasTag.asString().equals(name))
                        return true;
                }
            }

            if (!ignoreGroups)
                if (offlineTag.getValue().contains("PingGroups")) {
                    ListTag pingGroupsTag = offlineTag.getValue().getList("PingGroups", 8);
                    for (Tag pingGroup : pingGroupsTag) {
                        if (pingGroup.asString().equals(name))
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
            if (!possibleNames.contains(playerName))
                possibleNames.add(playerName);

            for (String alias : PlayerUtils.getAliasesOf(otherPlayer)) {
                if (!possibleNames.contains(alias))
                    possibleNames.add(alias);
            }

            for (String group : PlayerUtils.getPingGroupsOf(otherPlayer)) {
                if (!possibleNames.contains(group))
                    possibleNames.add(group);
            }
        }
        for (Map.Entry<UUID, CompoundTag> offlinePlayerTag : OfflinePlayerCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlinePlayerTag.getKey()) != null)
                continue;

            if (offlinePlayerTag.getValue().contains("SavedUsername")) {
                String offlineUsername = offlinePlayerTag.getValue().getString("SavedUsername");
                if (!possibleNames.contains(offlineUsername))
                    possibleNames.add(offlineUsername);
            }
            if (offlinePlayerTag.getValue().contains("Shortnames")) {
                ListTag aliasesTag = offlinePlayerTag.getValue().getList("Shortnames", 8);
                for (Tag aliasTag : aliasesTag) {
                    String alias = aliasTag.asString();
                    if (!possibleNames.contains(alias))
                        possibleNames.add(alias);
                }
            }
            if (offlinePlayerTag.getValue().contains("PingGroups")) {
                ListTag pingGroupsTag = offlinePlayerTag.getValue().getList("PingGroups", 8);
                for (Tag pingGroupTag : pingGroupsTag) {
                    String pingGroup = pingGroupTag.asString();
                    if (!possibleNames.contains(pingGroup))
                        possibleNames.add(pingGroup);
                }
            }
        }
        return possibleNames;
    }
}
