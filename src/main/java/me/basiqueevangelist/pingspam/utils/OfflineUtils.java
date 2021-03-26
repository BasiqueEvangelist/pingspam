package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.nevseti.nbt.CompoundTagView;
import me.basiqueevangelist.nevseti.nbt.ListTagView;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.util.dynamic.DynamicSerializableUuid;

import java.util.UUID;

public final class OfflineUtils {
    private OfflineUtils() {

    }

    public static boolean isPlayerIgnoredBy(CompoundTagView pingedTag, UUID sender) {
        if (pingedTag.contains("IgnoredPlayers")) {
            ListTagView ignoredPlayerListTag = pingedTag.getList("IgnoredPlayers", NbtType.INT_ARRAY);
            for (int i = 0; i < ignoredPlayerListTag.size(); i++) {
                if (DynamicSerializableUuid.toUuid (ignoredPlayerListTag.getIntArray(i)).equals(sender))
                    return true;
            }
        }
        return false;
    }
}
