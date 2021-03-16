package me.basiqueevangelist.pingspam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

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
}
