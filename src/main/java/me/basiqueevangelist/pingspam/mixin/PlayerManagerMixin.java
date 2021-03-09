package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.OfflinePlayerCache;
import me.basiqueevangelist.pingspam.PingLogic;
import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Shadow @Nullable public abstract ServerPlayerEntity getPlayer(String name);

    @Shadow @Nullable public abstract ServerPlayerEntity getPlayer(UUID uuid);

    @Shadow @Final private List<ServerPlayerEntity> players;

    @Shadow public abstract void sendToAll(Packet<?> packet);

    @Unique private static final Pattern PING_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{2,16})(\\s|$)");

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void onPlayerConnected(ClientConnection conn, ServerPlayerEntity player, CallbackInfo ci) {
        PacketByteBuf diffBuf = PacketByteBufs.create();
        List<String> shortnames = ((ServerPlayerEntityAccess) player).pingspam$getShortnames();
        diffBuf.writeVarInt(shortnames.size() + 1);
        diffBuf.writeString(player.getGameProfile().getName());
        for (String shortname : shortnames) {
            diffBuf.writeString(shortname);
        }
        diffBuf.writeVarInt(0);
        sendToAll(ServerPlayNetworking.createS2CPacket(PingSpamPackets.POSSIBLE_NAMES_DIFF, diffBuf));

        PacketByteBuf newBuf = PacketByteBufs.create();

        newBuf.writeBoolean(Permissions.check(player, "pingspam.pingeveryone", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.pingonline", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.pingoffline", 2));
        newBuf.writeBoolean(Permissions.check(player, "pingspam.pingplayer", 0));

        List<String> possibleNames = new ArrayList<>();
        for (ServerPlayerEntity otherPlayer : players) {
            String playerName = otherPlayer.getGameProfile().getName();
            if (!possibleNames.contains(playerName))
                possibleNames.add(playerName);

            for (String shortname : ((ServerPlayerEntityAccess) otherPlayer).pingspam$getShortnames()) {
                if (!possibleNames.contains(shortname))
                    possibleNames.add(shortname);
            }
        }
        for (Map.Entry<UUID, CompoundTag> offlinePlayerTag : OfflinePlayerCache.INSTANCE.getPlayers().entrySet()) {
            if (getPlayer(offlinePlayerTag.getKey()) != null)
                continue;

            if (offlinePlayerTag.getValue().contains("SavedUsername")) {
                String offlineUsername = offlinePlayerTag.getValue().getString("SavedUsername");
                if (!possibleNames.contains(offlineUsername))
                    possibleNames.add(offlineUsername);
            }
            if (offlinePlayerTag.getValue().contains("Shortnames")) {
                ListTag shortnamesTag = offlinePlayerTag.getValue().getList("Shortnames", 8);
                for (Tag shortnameTag : shortnamesTag) {
                    String shortname = shortnameTag.asString();
                    if (!possibleNames.contains(shortname))
                        possibleNames.add(shortname);
                }
            }
        }
        newBuf.writeVarInt(possibleNames.size());
        for (String possibleName : possibleNames) {
            newBuf.writeString(possibleName);
        }

        conn.send(ServerPlayNetworking.createS2CPacket(PingSpamPackets.ANNOUNCE, newBuf));
    }

    @Redirect(method = "broadcastChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/Packet;)V"))
    public void onMessageBroadcasted(PlayerManager playerManager, Packet<?> packet) {
        GameMessageS2CPacketAccessor access = (GameMessageS2CPacketAccessor) packet;
        String contents = access.pingspam$getMessage().getString();
        ServerPlayerEntity sender = getPlayer(access.pingspam$getSenderUuid());
        Matcher matcher = PING_PATTERN.matcher(contents);
        List<ServerPlayerEntity> unpingedPlayers = new ArrayList<>(players);
        List<UUID> pingedOfflinePlayers = new ArrayList<>();
        boolean pingSucceeded = false;
        while (matcher.find()) {
            String username = matcher.group(1);
            switch (username) {
                case "everyone":
                    if (sender == null || Permissions.check(sender, "pingspam.pingeveryone", 2)) {
                        for (ServerPlayerEntity player : players) {
                            if (unpingedPlayers.contains(player))
                                PingLogic.pingOnlinePlayer(
                                    player,
                                    access.pingspam$getMessage(),
                                    access.pingspam$getLocation(),
                                    access.pingspam$getSenderUuid());
                        }
                        for (UUID offlinePlayer : OfflinePlayerCache.INSTANCE.getPlayers().keySet()) {
                            if (getPlayer(offlinePlayer) != null)
                                continue;

                            if (!pingedOfflinePlayers.contains(offlinePlayer)) {
                                PingLogic.pingOfflinePlayer(offlinePlayer, access.pingspam$getMessage());
                                pingedOfflinePlayers.add(offlinePlayer);
                            }
                        }
                        unpingedPlayers.clear();
                        pingSucceeded = true;
                    } else {
                        PingLogic.sendPingError(sender, "You do not have enough permissions to ping @everyone!");
                    }
                    break;
                case "online":
                    if (sender == null || Permissions.check(sender, "pingspam.pingonline", 2)) {
                        for (ServerPlayerEntity player : players) {
                            if (unpingedPlayers.contains(player))
                                PingLogic.pingOnlinePlayer(
                                    player,
                                    access.pingspam$getMessage(),
                                    access.pingspam$getLocation(),
                                    access.pingspam$getSenderUuid());
                        }
                        unpingedPlayers.clear();
                        pingSucceeded = true;
                    } else {
                        PingLogic.sendPingError(sender, "You do not have enough permissions to ping @online!");
                    }
                    break;
                case "offline":
                    if (sender == null || Permissions.check(sender, "pingspam.pingoffline", 2)) {
                        for (UUID offlinePlayer : OfflinePlayerCache.INSTANCE.getPlayers().keySet()) {
                            if (getPlayer(offlinePlayer) != null)
                                continue;

                            if (!pingedOfflinePlayers.contains(offlinePlayer)) {
                                PingLogic.pingOfflinePlayer(offlinePlayer, access.pingspam$getMessage());
                                pingedOfflinePlayers.add(offlinePlayer);
                            }
                        }
                        pingSucceeded = true;
                    } else {
                        PingLogic.sendPingError(sender, "You do not have enough permissions to ping @offline!");
                    }
                    break;
                default:
                    if (sender == null || Permissions.check(sender, "pingspam.pingplayer", 0)) {
                        pingSucceeded = pingSucceeded || PingLogic.pingPlayerByNameWithResponse(
                            (PlayerManager) (Object) this,
                            username,
                            access.pingspam$getMessage(),
                            access.pingspam$getLocation(),
                            access.pingspam$getSenderUuid(),
                            sender);
                    } else {
                        PingLogic.sendPingError(sender, "You do not have enough permissions to ping @" + username + "!");
                    }
                    break;
            }
        }

        if (pingSucceeded && sender != null) {
            if (unpingedPlayers.contains(sender)) {
                sender.networkHandler.sendPacket(new GameMessageS2CPacket(
                    access.pingspam$getMessage().shallowCopy().formatted(Formatting.GOLD),
                    access.pingspam$getLocation(),
                    access.pingspam$getSenderUuid()
                ));
                unpingedPlayers.remove(sender);
            }
        }

        for (ServerPlayerEntity player : unpingedPlayers) {
            player.networkHandler.sendPacket(packet);
        }
    }
}
