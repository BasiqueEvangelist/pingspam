package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.nevseti.OfflineDataCache;
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

            List<String> aliases = getAliasesOf(player);
            if (aliases.contains(name))
                return player;
        }

        return null;
    }

    public static @Nullable UUID findOfflinePlayer(PlayerManager manager, String name) {
        for (Map.Entry<UUID, CompoundTag> offlineTag : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("SavedUsername") && offlineTag.getValue().getString("SavedUsername").equals(name)) {
                return offlineTag.getKey();
            }

            if (offlineTag.getValue().contains("Shortnames")) {
                ListTag aliasesTag = offlineTag.getValue().getList("Shortnames", 8);
                for (Tag aliasTag : aliasesTag) {
                    if (aliasTag.asString().equals(name))
                        return offlineTag.getKey();
                }
            }
        }
        return null;
    }

    public static PlayerList queryPingGroup(PlayerManager manager, String name) {
        PlayerList list = new PlayerList();

        for (ServerPlayerEntity player : manager.getPlayerList()) {
            List<String> groups = getPingGroupsOf(player);
            if (groups.contains(name))
                list.getOnlinePlayers().add(player);
        }

        for (Map.Entry<UUID, CompoundTag> offlineTag : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("PingGroups")) {
                ListTag pingGroupsTag = offlineTag.getValue().getList("PingGroups", 8);
                for (Tag pingGroupTag : pingGroupsTag) {
                    if (pingGroupTag.asString().equals(name))
                        list.getOfflinePlayers().add(offlineTag.getKey());
                }
            }
        }

        return list;
    }

    public static List<Text> getUnreadPingsFor(ServerPlayerEntity player) {
        return ((ServerPlayerEntityAccess) player).pingspam$getPings();
    }

    public static List<String> getAliasesOf(ServerPlayerEntity player) {
        return ((ServerPlayerEntityAccess) player).pingspam$getAliases();
    }

    public static List<String> getPingGroupsOf(ServerPlayerEntity player) {
        return ((ServerPlayerEntityAccess) player).pingspam$getPingGroups();
    }

    public static SoundEvent getPingSound(ServerPlayerEntity player) {
        return ((ServerPlayerEntityAccess) player).pingspam$getPingSound();
    }

    public static void setPingSound(ServerPlayerEntity player, SoundEvent sound) {
        ((ServerPlayerEntityAccess) player).pingspam$setPingSound(sound);
    }

    public static List<UUID> getIgnoredPlayersOf(ServerPlayerEntity player) {
        return ((ServerPlayerEntityAccess) player).pingspam$getIgnoredPlayers();
    }
}
