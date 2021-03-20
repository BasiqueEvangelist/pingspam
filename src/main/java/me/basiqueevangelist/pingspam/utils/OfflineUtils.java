package me.basiqueevangelist.pingspam.utils;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public final class OfflineUtils {
    private OfflineUtils() {

    }

    public static boolean isPlayerIgnoredBy(CompoundTag pingedTag, UUID sender) {
        if (pingedTag.contains("IgnoredPlayers")) {
            ListTag ignoredPlayerListTag = pingedTag.getList("IgnoredPlayers", NbtType.INT_ARRAY);
            for (Tag ignoredPlayerTag : ignoredPlayerListTag) {
                if (NbtHelper.toUuid(ignoredPlayerTag).equals(sender))
                    return true;
            }
        }
        return false;
    }

    public static String getSavedUsername(CompoundTag tag) {
        return tag.contains("SavedUsername") ? tag.getString("SavedUsername") : null;
    }
}
