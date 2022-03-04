package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.pingspam.data.PingspamPersistentState;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
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

        for (Map.Entry<UUID, PingspamPlayerData> entry : PingspamPersistentState.getFrom(server).getPlayerMap().entrySet()) {
            if (NameUtil.getNameFromUUID(entry.getKey()).equalsIgnoreCase(name))
                return entry.getKey();

            for (String alias : entry.getValue().aliases()) {
                if (alias.equalsIgnoreCase(name))
                    return entry.getKey();
            }
        }

        return null;
    }

    public static Set<UUID> getAllPlayers(MinecraftServer server) {
        Set<UUID> players = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            players.add(player.getUuid());
        }

        players.addAll(PingspamPersistentState.getFrom(server).getPlayerMap().keySet());

        return players;
    }
}
