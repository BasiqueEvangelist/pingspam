package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.OfflinePlayerCache;
import me.basiqueevangelist.pingspam.PingLogic;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.PlayerUtils;
import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
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
        ServerNetworkLogic.addPossibleName((PlayerManager)(Object) this, player.getGameProfile().getName());
        ServerNetworkLogic.sendServerAnnouncement(player, conn);
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
        if (PingSpam.CONFIG.getConfig().processPingsFromUnknownPlayers || sender != null) {
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
                        ServerPlayerEntity onlinePlayer = PlayerUtils.findOnlinePlayer((PlayerManager) (Object) this, username);
                        if (sender != null && !Permissions.check(sender, "pingspam.overrideignore", 2)) {
                            if (onlinePlayer != null && PingLogic.pingedOnlineUserIgnoredBySender(sender.getUuid(), (ServerPlayerEntityAccess) onlinePlayer)) {
                                sender.sendMessage(new LiteralText(onlinePlayer.getEntityName() + " has ignored you, they won't receive your ping.").formatted(Formatting.RED), false);
                                break;
                            }
                            //TODO: Block pings to offline users that have ignored the sender
                        }

                        if (sender == null || Permissions.check(sender, "pingspam.pingplayer", 0)) {
                            if (onlinePlayer != null) {
                                if (unpingedPlayers.contains(onlinePlayer)) {
                                    PingLogic.pingOnlinePlayer(
                                        onlinePlayer,
                                        access.pingspam$getMessage(),
                                        access.pingspam$getLocation(),
                                        access.pingspam$getSenderUuid());
                                    pingSucceeded = true;
                                    unpingedPlayers.remove(onlinePlayer);
                                }
                            } else {
                                UUID offlinePlayer = PlayerUtils.findOfflinePlayer((PlayerManager) (Object) this, username);
                                if (offlinePlayer != null) {
                                    if (!pingedOfflinePlayers.contains(offlinePlayer)) {
                                        PingLogic.pingOfflinePlayer(offlinePlayer, access.pingspam$getMessage());
                                        pingSucceeded = true;
                                        pingedOfflinePlayers.add(offlinePlayer);
                                    }
                                } else if (sender != null)
                                    PingLogic.sendPingError(sender, "No such player: " + username + "!");
                            }

                        } else {
                            PingLogic.sendPingError(sender, "You do not have enough permissions to ping @" + username + "!");
                        }
                        break;
                }
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
