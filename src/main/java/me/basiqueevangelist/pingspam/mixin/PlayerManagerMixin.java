package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
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
    @Unique private static final Pattern PING_PATTERN = Pattern.compile("@([a-zA-Z0-9]{3,16}(\\s|$))");

    @Redirect(method = "broadcastChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/Packet;)V"))
    public void onMessageBroadcasted(PlayerManager playerManager, Packet<?> untypedPacket) {
        GameMessageS2CPacket packet = (GameMessageS2CPacket)untypedPacket;
        String contents = packet.getMessage().getString();
        Matcher matcher = PING_PATTERN.matcher(contents);
        List<ServerPlayerEntity> unpingedPlayers = new ArrayList<>(players);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (username.equals("everyone")) {
                ServerPlayerEntity sender = getPlayer(packet.getSenderUuid());
                if (sender != null && !sender.hasPermissionLevel(2))
                    continue;
                for (ServerPlayerEntity player : players) {
                    ((ServerPlayerEntityAccess)player).pingspam$ping(packet);
                }
                unpingedPlayers.clear();
            } else {
                ServerPlayerEntity player = getPlayer(username);
                if (player != null) {
                    ((ServerPlayerEntityAccess) player).pingspam$ping(packet);
                    unpingedPlayers.remove(player);
                }
            }
        }

        for (ServerPlayerEntity player : unpingedPlayers) {
            player.networkHandler.sendPacket(packet);
        }
    }
}
