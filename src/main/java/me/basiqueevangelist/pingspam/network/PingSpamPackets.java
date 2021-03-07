package me.basiqueevangelist.pingspam.network;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class PingSpamPackets {
    public static final Identifier ANNOUNCE = new Identifier("pingspam", "announce");
    public static final Identifier PULL_PERMISSIONS = new Identifier("pingspam", "pull_permissions");
    public static final Identifier POSSIBLE_NAMES_DIFF = new Identifier("pingspam", "possible_names_diff");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PULL_PERMISSIONS, (server, player, handler, buf, responseSender) -> {
            PacketByteBuf newBuf = PacketByteBufs.create();

            newBuf.writeBoolean(Permissions.check(player, "pingspam.pingeveryone", 2));
            newBuf.writeBoolean(Permissions.check(player, "pingspam.pingplayer", 0));

            responseSender.sendPacket(PULL_PERMISSIONS, newBuf);
        });
    }
}
