package me.basiqueevangelist.pingspam;

import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerUtils {
    private PlayerUtils() {

    }

    public static @Nullable ServerPlayerEntity findOnlinePlayer(PlayerManager manager, String name) {
        for (ServerPlayerEntity player : manager.getPlayerList()) {
            if (player.getGameProfile().getName().equals(name))
                return player;

            List<String> shortnames = getShortnamesOf(player);
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
        return findOnlinePlayer(manager, name) != null || findOfflinePlayer(manager, name) != null;
    }

    public static List<Text> getUnreadPingsFor(ServerPlayerEntity player) {
        return ((ServerPlayerEntityAccess) player).pingspam$getPings();
    }

    public static List<String> getShortnamesOf(ServerPlayerEntity player) {
        return ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
    }

    public static SoundEvent getPingSound(ServerPlayerEntity player) {
        return ((ServerPlayerEntityAccess) player).pingspam$getPingSound();
    }

    public static void setPingSound(ServerPlayerEntity player, SoundEvent sound) {
        ((ServerPlayerEntityAccess) player).pingspam$setPingSound(sound);
    }
}
