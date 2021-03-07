package me.basiqueevangelist.pingspam;

import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PlayerUtils {
    private PlayerUtils() {

    }

    public static @Nullable ServerPlayerEntity findPlayer(PlayerManager manager, String name) {
        for (ServerPlayerEntity player : manager.getPlayerList()) {
            if (player.getGameProfile().getName().equals(name))
                return player;

            List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
            if (shortnames.contains(name))
                return player;
        }

        return null;
    }
}
