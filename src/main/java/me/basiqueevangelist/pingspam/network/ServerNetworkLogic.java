package me.basiqueevangelist.pingspam.network;

import me.basiqueevangelist.pingspam.OfflinePlayerCache;
import me.basiqueevangelist.pingspam.PlayerUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ServerNetworkLogic {
    private ServerNetworkLogic() {

    }

    public static void sendServerAnnouncement(ServerPlayerEntity player, ClientConnection conn) {
        PlayerManager manager = player.server.getPlayerManager();
        PacketByteBuf newBuf = PacketByteBufs.create();

        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.everyone", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.online", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.offline", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.ping.player", true));

        List<String> possibleNames = new ArrayList<>();
        for (ServerPlayerEntity otherPlayer : manager.getPlayerList()) {
            String playerName = otherPlayer.getGameProfile().getName();
            if (!possibleNames.contains(playerName))
                possibleNames.add(playerName);

            for (String alias : PlayerUtils.getAliasesOf(otherPlayer)) {
                if (!possibleNames.contains(alias))
                    possibleNames.add(alias);
            }
        }
        for (Map.Entry<UUID, CompoundTag> offlinePlayerTag : OfflinePlayerCache.INSTANCE.getPlayers().entrySet()) {
            if (manager.getPlayer(offlinePlayerTag.getKey()) != null)
                continue;

            if (offlinePlayerTag.getValue().contains("SavedUsername")) {
                String offlineUsername = offlinePlayerTag.getValue().getString("SavedUsername");
                if (!possibleNames.contains(offlineUsername))
                    possibleNames.add(offlineUsername);
            }
            if (offlinePlayerTag.getValue().contains("Shortnames")) {
                ListTag aliasesTag = offlinePlayerTag.getValue().getList("Shortnames", 8);
                for (Tag aliasTag : aliasesTag) {
                    String alias = aliasTag.asString();
                    if (!possibleNames.contains(alias))
                        possibleNames.add(alias);
                }
            }
        }
        newBuf.writeVarInt(possibleNames.size());
        for (String possibleName : possibleNames) {
            newBuf.writeString(possibleName);
        }

        conn.send(ServerPlayNetworking.createS2CPacket(PingSpamPackets.ANNOUNCE, newBuf));
    }

    public static void removePossibleName(PlayerManager manager, String possibleName) {
        PacketByteBuf diffBuf = PacketByteBufs.create();
        diffBuf.writeVarInt(0);
        diffBuf.writeVarInt(1);
        diffBuf.writeString(possibleName);
        manager.sendToAll(ServerPlayNetworking.createS2CPacket(PingSpamPackets.POSSIBLE_NAMES_DIFF, diffBuf));
    }

    public static void addPossibleName(PlayerManager manager, String possibleName) {
        PacketByteBuf diffBuf = PacketByteBufs.create();
        diffBuf.writeVarInt(1);
        diffBuf.writeString(possibleName);
        diffBuf.writeVarInt(0);
        manager.sendToAll(ServerPlayNetworking.createS2CPacket(PingSpamPackets.POSSIBLE_NAMES_DIFF, diffBuf));
    }
}
