package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.nevseti.OfflineDataCache;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class PlayerList {
    private final List<ServerPlayerEntity> onlinePlayers = new ArrayList<>();
    private final List<UUID> offlinePlayers = new ArrayList<>();

    public PlayerList() {

    }

    public static PlayerList fromAllPlayers(PlayerManager manager) {
        PlayerList list = new PlayerList();
        list.onlinePlayers.addAll(manager.getPlayerList());
        for (UUID offlinePlayer : OfflineDataCache.INSTANCE.getPlayers().keySet()) {
            if (manager.getPlayer(offlinePlayer) != null)
                continue;

            list.offlinePlayers.add(offlinePlayer);
        }

        return list;
    }

    public static PlayerList fromOnline(PlayerManager manager) {
        PlayerList list = new PlayerList();
        list.onlinePlayers.addAll(manager.getPlayerList());
        return list;
    }

    public static PlayerList fromOffline(PlayerManager manager) {
        PlayerList list = new PlayerList();
        for (UUID offlinePlayer : OfflineDataCache.INSTANCE.getPlayers().keySet()) {
            if (manager.getPlayer(offlinePlayer) != null)
                continue;

            list.offlinePlayers.add(offlinePlayer);
        }
        return list;
    }

    public List<ServerPlayerEntity> getOnlinePlayers() {
        return onlinePlayers;
    }

    public List<UUID> getOfflinePlayers() {
        return offlinePlayers;
    }

    public void add(ServerPlayerEntity onlinePlayer) {
        onlinePlayers.add(onlinePlayer);
    }

    public void add(UUID offlinePlayer) {
        offlinePlayers.add(offlinePlayer);
    }

    public boolean isEmpty() {
        return onlinePlayers.isEmpty() && offlinePlayers.isEmpty();
    }
}
