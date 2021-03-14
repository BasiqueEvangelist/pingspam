package me.basiqueevangelist.pingspam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

public final class ShortnameLogic {
    private ShortnameLogic() {

    }

    public static boolean checkForCollision(PlayerManager manager, String shortname) {
        for (ServerPlayerEntity onlinePlayer : manager.getPlayerList()) {
            for (String otherShortname : PlayerUtils.getShortnamesOf(onlinePlayer)) {
                if (otherShortname.equals(shortname))
                    return true;
            }
        }

        for (Map.Entry<UUID, CompoundTag> offlineTag : OfflinePlayerCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;
            if (offlineTag.getValue().contains("Shortnames")) {
                ListTag shortnamesTag = offlineTag.getValue().getList("Shortnames", 8);
                for (Tag shortnameTag : shortnamesTag) {
                    if (shortnameTag.asString().equals(shortname))
                        return true;
                }
            }
        }

        return false;
    }
}
