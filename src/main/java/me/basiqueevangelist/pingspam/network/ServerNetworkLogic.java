package me.basiqueevangelist.pingspam.network;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.logic.NameLogic;
import me.basiqueevangelist.pingspam.logic.PingspamPermissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Set;

public final class ServerNetworkLogic {
    private ServerNetworkLogic() {

    }

    public static void sendServerAnnouncement(ServerPlayerEntity player, ClientConnection conn) {
        DataStore store = DataStore.getFor(player.getWorld().getServer());
        var data = store.getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        PacketByteBuf newBuf = PacketByteBufs.create();

        Set<String> possibleNames;
        var group = data.currentChat() == null ? null : store.get(PingSpam.GLOBAL_DATA).groups().get(data.currentChat());

        if (group != null) {
            possibleNames = NameLogic.listValidNames(player.server, group.members()::contains, false);
        } else {
            possibleNames = NameLogic.listValidNames(player.server, uuid -> true, true);
        }

        newBuf.writeCollection(possibleNames, PacketByteBuf::writeString);

        var payload = new AnnounceS2CPayload(
            PingspamPermissions.pingEveryone(player),
            PingspamPermissions.pingOnline(player),
            PingspamPermissions.pingOffline(player),
            PingspamPermissions.pingPlayer(player),
            possibleNames
        );

        conn.send(ServerPlayNetworking.createS2CPacket(payload));
    }

    public static void removePossibleName(PlayerManager manager, String possibleName) {
        sendToAll(manager, new PossibleNamesDiffS2CPacket(List.of(), List.of(possibleName)));
    }

    public static void addPossibleName(PlayerManager manager, String possibleName) {
        sendToAll(manager, new PossibleNamesDiffS2CPacket(List.of(possibleName), List.of()));
    }

    public static void sendToAll(PlayerManager manager, CustomPayload payload) {
        Packet<?> packet = ServerPlayNetworking.createS2CPacket(payload);
        for (ServerPlayerEntity player : manager.getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, payload.getId()) || PingSpam.CONFIG.getConfig().ignoreCanSend)
                player.networkHandler.sendPacket(packet);
        }
    }
}
