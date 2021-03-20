package me.basiqueevangelist.pingspam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AliasLogic {
    private AliasLogic() {

    }

    public static boolean checkForCollision(PlayerManager manager, String alias) {
        for (ServerPlayerEntity onlinePlayer : manager.getPlayerList()) {
            for (String otherAlias : PlayerUtils.getAliasesOf(onlinePlayer)) {
                if (otherAlias.equals(alias))
                    return true;
            }
        }

        for (Map.Entry<UUID, CompoundTag> offlineTag : OfflinePlayerCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;
            if (offlineTag.getValue().contains("Shortnames")) {
                ListTag aliasesTag = offlineTag.getValue().getList("Shortnames", 8);
                for (Tag aliasTag : aliasesTag) {
                    if (aliasTag.asString().equals(alias))
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
        }
        return possibleNames;
    }
}
