package me.basiqueevangelist.pingspam;

import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerUtils {
    private PlayerUtils() {

    }

    public static @Nullable ServerPlayerEntity findPlayer(PlayerManager manager, String name) {
        for (ServerPlayerEntity player : manager.getPlayerList()) {
            if (player.getGameProfile().getName().equals(name))
                return player;

            List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
            if (shortnames.contains(name))
                return player;
        }

        return null;
    }

    public static @Nullable UUID findOfflinePlayer(PlayerManager manager, String name) {
        for (Map.Entry<UUID, CompoundTag> offlineTag : OfflinePlayerCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("SavedUsername") && offlineTag.getValue().getString("SavedUsername").equals(name)) {
                return offlineTag.getKey();
            }

            if (offlineTag.getValue().contains("Shortnames")) {
                ListTag shortnamesTag = offlineTag.getValue().getList("Shortnames", 8);
                for (Tag shortnameTag : shortnamesTag) {
                    if (shortnameTag.asString().equals(name))
                        return offlineTag.getKey();
                }
            }
        }
        return null;
    }

    public static boolean anyPlayer(PlayerManager manager, String name) {
        return findPlayer(manager, name) != null || findOfflinePlayer(manager, name) != null;
    }
}
