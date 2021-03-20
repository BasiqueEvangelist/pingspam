package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.basiqueevangelist.pingspam.utils.PingLogic;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Shadow @Final private List<ServerPlayerEntity> players;


    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void onPlayerConnected(ClientConnection conn, ServerPlayerEntity player, CallbackInfo ci) {
        ServerNetworkLogic.addPossibleName((PlayerManager)(Object) this, player.getGameProfile().getName());
        ServerNetworkLogic.sendServerAnnouncement(player, conn);
    }

    @Redirect(method = "broadcastChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/Packet;)V"))
    public void onMessageBroadcasted(PlayerManager playerManager, Packet<?> packet) {
        GameMessageS2CPacketAccessor access = (GameMessageS2CPacketAccessor) packet;

        PingLogic.ProcessedPing result = PingLogic.processPings(playerManager, access.pingspam$getMessage(), access.pingspam$getLocation(), access.pingspam$getSenderUuid());

        if (result.pingSucceeded && result.sender != null) {
            if (!result.pingedPlayers.getOnlinePlayers().contains(result.sender)) {
                result.sender.networkHandler.sendPacket(new GameMessageS2CPacket(
                    access.pingspam$getMessage().shallowCopy().formatted(Formatting.GOLD),
                    access.pingspam$getLocation(),
                    access.pingspam$getSenderUuid()
                ));
                result.pingedPlayers.add(result.sender);
            }
        }

        for (ServerPlayerEntity player : players) {
            if (!result.pingedPlayers.getOnlinePlayers().contains(player))
                player.networkHandler.sendPacket(packet);
        }
    }
}
