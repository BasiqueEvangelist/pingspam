package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.pingspam.PingSpam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerUtils {
    private PlayerUtils() {

    }

    public static @Nullable UUID tryFindPlayer(MinecraftServer server, String name) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(name))
                return player.getUuid();
        }

        for (PlayerDataEntry entry : DataStore.getFor(server).players()) {
            if (NameUtil.getNameFromUUID(entry.playerId()).equalsIgnoreCase(name))
                return entry.playerId();

            if (entry.get(PingSpam.PLAYER_DATA).aliases().contains(name))
                return entry.playerId();
        }

        return null;
    }

    public static Set<UUID> getAllPlayers(MinecraftServer server) {
        Set<UUID> players = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            players.add(player.getUuid());
        }

        for (PlayerDataEntry entry : DataStore.getFor(server).players()) {
            players.add(entry.playerId());
        }

        return players;
    }
}
