package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.basiqueevangelist.pingspam.utils.PingLogic;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Function;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Shadow @Final private MinecraftServer server;
    @Unique private PingLogic.ProcessedPing pong;

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void onPlayerConnected(ClientConnection conn, ServerPlayerEntity player, CallbackInfo ci) {
        ServerNetworkLogic.addPossibleName((PlayerManager)(Object) this, player.getGameProfile().getName());
        ServerNetworkLogic.sendServerAnnouncement(player, conn);
    }

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;sendSystemMessage(Lnet/minecraft/text/Text;Ljava/util/UUID;)V", shift = At.Shift.AFTER))
    public void processPing(Text message, MessageType type, UUID sender, CallbackInfo ci) {
        pong = PingLogic.processPings(server, message, type, sender);
    }

    @Inject(method = "broadcast*", at = @At("RETURN"))
    private void deletePing(CallbackInfo ci) {
        pong = null;
    }

    @Redirect(method = "broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
    private void sendMessage(ServerPlayerEntity player, Text message, MessageType type, UUID sender) {
        if (pong.pingSucceeded && pong.sender == player) {
            if (!pong.pingedPlayers.contains(pong.sender.getUuid())) {
                pong.sender.networkHandler.sendPacket(new GameMessageS2CPacket(
                    message.shallowCopy().formatted(Formatting.GOLD),
                    type,
                    sender
                ));
                pong.pingedPlayers.add(pong.sender.getUuid());
            }
        }

        if (!pong.pingedPlayers.contains(player.getUuid()))
            player.sendMessage(message, type, sender);
    }

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;sendSystemMessage(Lnet/minecraft/text/Text;Ljava/util/UUID;)V", shift = At.Shift.AFTER))
    private void processPingFilter(Text serverMessage, Function<ServerPlayerEntity, Text> playerMessageFactory, MessageType playerMessageType, UUID sender, CallbackInfo ci) {
        pong = PingLogic.processPings(server, serverMessage, playerMessageType, sender);
    }

    @Redirect(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
    private void sendMessageFilter(ServerPlayerEntity player, Text message, MessageType type, UUID sender, Text serverMessage, Function<ServerPlayerEntity, Text> playerMessageFactory, MessageType playerMessageType, UUID sender2) {
        if (pong.pingSucceeded && pong.sender == player) {
            if (!pong.pingedPlayers.contains(pong.sender.getUuid())) {
                pong.sender.networkHandler.sendPacket(new GameMessageS2CPacket(
                    message.shallowCopy().formatted(Formatting.GOLD),
                    type,
                    sender
                ));
                pong.pingedPlayers.add(pong.sender.getUuid());
            }
        }

        if (!pong.pingedPlayers.contains(player.getUuid()))
            player.sendMessage(message, type, sender);
    }
}
