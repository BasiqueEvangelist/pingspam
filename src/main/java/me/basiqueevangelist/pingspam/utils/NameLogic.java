package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.pingspam.data.PingspamPersistentState;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NameLogic {
    private NameLogic() {

    }

    public static boolean isValidName(MinecraftServer server, String name, boolean ignoreGroups) {
        if (name.equals("everyone") || name.equals("online") || name.equals("offline"))
            return true;

        for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
            if (onlinePlayer.getGameProfile().getName().equalsIgnoreCase(name))
                return true;
        }

        PingspamPersistentState state = PingspamPersistentState.getFrom(server);

        for (Map.Entry<UUID, PingspamPlayerData> entry : state.getPlayerMap().entrySet()) {
            if (NameUtil.getNameFromUUID(entry.getKey()).equalsIgnoreCase(name))
                return true;

            for (var alias : entry.getValue().aliases())
                if (alias.equalsIgnoreCase(name))
                        return true;
        }

        if (!ignoreGroups)
            for (String groupName : state.getGroups().keySet()) {
                if (groupName.equalsIgnoreCase(name))
                    return true;
            }

        return false;
    }

    public static Set<String> listValidNames(MinecraftServer server) {
        Set<String> possibleNames = CaseInsensitiveUtil.setIgnoringCase();

        for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
            String playerName = otherPlayer.getGameProfile().getName();
            possibleNames.add(playerName);
        }

        for (Map.Entry<UUID, PingspamPlayerData> entry : PingspamPersistentState.getFrom(server).getPlayerMap().entrySet()) {
            String name = NameUtil.getNameFromUUIDOrNull(entry.getKey());

            if (name != null)
                possibleNames.add(name);

            possibleNames.addAll(entry.getValue().aliases());
        }

        possibleNames.addAll(PingspamPersistentState.getFrom(server).getGroups().keySet());

        return possibleNames;
    }
}
