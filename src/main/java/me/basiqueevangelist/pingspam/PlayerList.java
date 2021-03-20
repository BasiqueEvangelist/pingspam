package me.basiqueevangelist.pingspam;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerList {
    private final List<ServerPlayerEntity> onlinePlayers = new ArrayList<>();
    private final List<UUID> offlinePlayers = new ArrayList<>();

    public PlayerList() {

    }

    public List<ServerPlayerEntity> getOnlinePlayers() {
        return onlinePlayers;
    }

    public List<UUID> getOfflinePlayers() {
        return offlinePlayers;
    }

    public boolean isEmpty() {
        return onlinePlayers.isEmpty() && offlinePlayers.isEmpty();
    }
}
