package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
    @Unique private static final Pattern PING_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{2,16})(\\s|$)");

    @Redirect(method = "broadcastChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/Packet;)V"))
    public void onMessageBroadcasted(PlayerManager playerManager, Packet<?> packet) {
        GameMessageS2CPacketAccessor access = (GameMessageS2CPacketAccessor) packet;
        String contents = access.pingspam$getMessage().getString();
        Matcher matcher = PING_PATTERN.matcher(contents);
        List<ServerPlayerEntity> unpingedPlayers = new ArrayList<>(players);
        while (matcher.find()) {
            String username = matcher.group(1);
            ServerPlayerEntity sender = getPlayer(access.pingspam$getSenderUuid());
            if (username.equals("everyone")) {
                if (sender == null || Permissions.check(sender, "pingspam.pingeveryone", 2)) {
                    for (ServerPlayerEntity player : players) {
                        ((ServerPlayerEntityAccess) player).pingspam$ping((GameMessageS2CPacket) access);
                    }
                    unpingedPlayers.clear();
                } else {
                    sendPingError(sender, "You do not have enough permissions to ping @everyone!");
                }
            } else {
                if (sender == null || Permissions.check(sender, "pingspam.pingplayer", 0)) {
                    ServerPlayerEntity player = findPlayer(username);
                    if (player != null) {
                        ((ServerPlayerEntityAccess) player).pingspam$ping((GameMessageS2CPacket) access);
                        unpingedPlayers.remove(player);
                    } else if (sender != null) {
                        sendPingError(sender, "No such player: " + username + "!");
                    }
                } else {
                    sendPingError(sender, "You do not have enough permissions to ping @" + username + "!");
                }
            }
        }

        for (ServerPlayerEntity player : unpingedPlayers) {
            player.networkHandler.sendPacket(packet);
        }
    }

    @Unique
    private void sendPingError(ServerPlayerEntity player, String text) {
        if (PingSpam.CONFIG.getConfig().sendPingErrors)
            player.sendSystemMessage(new LiteralText(text).formatted(Formatting.RED), Util.NIL_UUID);
    }

    @Unique
    private @Nullable ServerPlayerEntity findPlayer(String name) {
        ServerPlayerEntity namedPlayer = getPlayer(name);
        if (namedPlayer != null)
            return namedPlayer;

        for (ServerPlayerEntity player : players) {
            List<String> shortnames = ((ServerPlayerEntityAccess)player).pingspam$getShortnames();
            if (shortnames.contains(name))
                return player;
        }

        return null;
    }
}
