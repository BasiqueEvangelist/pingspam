package me.basiqueevangelist.pingspam.api;

import me.basiqueevangelist.pingspam.utils.PingLogic;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.UUID;

public final class PingspamApiV0 {
    private PingspamApiV0() {

    }

    public static void sendNotificationTo(MinecraftServer server, UUID playerId, Text message) {
        PingLogic.sendNotification(server, playerId, message);
    }
}
