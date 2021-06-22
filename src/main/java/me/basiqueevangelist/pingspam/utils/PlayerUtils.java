package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.basiqueevangelist.nevseti.nbt.CompoundTagView;
import me.basiqueevangelist.nevseti.nbt.ListTagView;
import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
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

    public static @Nullable UUID tryFindPlayer(PlayerManager manager, String name) {
        for (ServerPlayerEntity player : manager.getPlayerList()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(name))
                return player.getUuid();
        }

        for (Map.Entry<UUID, CompoundTagView> offlineTag : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("SavedUsername") && offlineTag.getValue().getString("SavedUsername").equalsIgnoreCase(name)) {
                return offlineTag.getKey();
            }
        }

        for (ServerPlayerEntity player : manager.getPlayerList()) {
            List<String> aliases = getAliasesOf(player);
            if (aliases.stream().anyMatch(x -> x.equalsIgnoreCase(name)))
                return player.getUuid();
        }

        for (Map.Entry<UUID, CompoundTagView> offlineTag : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("Shortnames")) {
                ListTagView aliasesTag = offlineTag.getValue().getList("Shortnames", 8);
                for (int i = 0; i < aliasesTag.size(); i++) {
                    if (aliasesTag.getString(i).equalsIgnoreCase(name))
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
            if (groups.stream().anyMatch(x -> x.equalsIgnoreCase(name)))
                list.getOnlinePlayers().add(player);
        }

        for (Map.Entry<UUID, CompoundTagView> offlineTag : OfflineDataCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlineTag.getKey()) != null)
                continue;

            if (offlineTag.getValue().contains("PingGroups")) {
                ListTagView pingGroupsTag = offlineTag.getValue().getList("PingGroups", 8);
                for (int i = 0; i < pingGroupsTag.size(); i++) {
                    if (pingGroupsTag.getString(i).equalsIgnoreCase(name))
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
