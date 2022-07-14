package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.basiqueevangelist.pingspam.utils.MessageTypeTransformer;
import me.basiqueevangelist.pingspam.utils.PingLogic;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageSender;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Function;Lnet/minecraft/network/message/MessageSender;Lnet/minecraft/util/registry/RegistryKey;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;logChatMessage(Lnet/minecraft/network/message/MessageSender;Lnet/minecraft/text/Text;)V", shift = At.Shift.AFTER))
    public void processPingSigned(SignedMessage message, Function<ServerPlayerEntity, SignedMessage> playerMessageFactory, MessageSender sender, RegistryKey<MessageType> typeKey, CallbackInfo ci) {
        MessageType msgType = server.getRegistryManager().get(Registry.MESSAGE_TYPE_KEY).get(typeKey);

        if (msgType == null)
            return;

        MessageType.DisplayRule rule = msgType.chat().orElse(null);

        if (rule != null) {
            pong = PingLogic.processPings(server, message.getContent(), rule.apply(message.getContent(), sender), typeKey, sender.uuid());
        }
    }

    @Inject(method = "broadcast*", at = @At("RETURN"))
    private void deletePing(CallbackInfo ci) {
        pong = null;
    }

    @Redirect(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Function;Lnet/minecraft/network/message/MessageSender;Lnet/minecraft/util/registry/RegistryKey;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendChatMessage(Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/network/message/MessageSender;Lnet/minecraft/util/registry/RegistryKey;)V"))
    private void sendMessageSigned(ServerPlayerEntity player, SignedMessage message, MessageSender sender, RegistryKey<MessageType> typeKey) {
        if (pong != null && pong.pingSucceeded) {
            if (pong.pingedPlayers.contains(player.getUuid())) {
                player.sendChatMessage(message, sender, RegistryKey.of(Registry.MESSAGE_TYPE_KEY, MessageTypeTransformer.wrapPinged(typeKey.getValue())));
            } else if (pong.sender == player) {
                player.sendChatMessage(message, sender, RegistryKey.of(Registry.MESSAGE_TYPE_KEY, MessageTypeTransformer.wrapPingSuccessful(typeKey.getValue())));
                pong.pingedPlayers.add(pong.sender.getUuid());
            }
        }

        if (pong == null || !pong.pingedPlayers.contains(player.getUuid()))
            player.sendChatMessage(message, sender, typeKey);
    }

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/util/registry/RegistryKey;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;sendMessage(Lnet/minecraft/text/Text;)V", shift = At.Shift.AFTER))
    private void processPing(Text message, Function<ServerPlayerEntity, Text> playerMessageFactory, RegistryKey<MessageType> typeKey, CallbackInfo ci) {
        MessageType msgType = server.getRegistryManager().get(Registry.MESSAGE_TYPE_KEY).get(typeKey);

        if (msgType == null)
            return;

        MessageType.DisplayRule rule = msgType.chat().orElse(null);

        if (rule != null) {
            pong = PingLogic.processPings(server, message, rule.apply(message, null), typeKey, Util.NIL_UUID);
        }
    }

    @Redirect(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/util/registry/RegistryKey;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/util/registry/RegistryKey;)V"))
    private void sendMessage(ServerPlayerEntity player, Text message, RegistryKey<MessageType> typeKey) {
        if (pong != null && pong.pingSucceeded) {
            if (pong.pingedPlayers.contains(player.getUuid())) {
                player.sendMessage(message, RegistryKey.of(Registry.MESSAGE_TYPE_KEY, MessageTypeTransformer.wrapPinged(typeKey.getValue())));
            } else if (pong.sender == player) {
                player.sendMessage(message, RegistryKey.of(Registry.MESSAGE_TYPE_KEY, MessageTypeTransformer.wrapPingSuccessful(typeKey.getValue())));
                pong.pingedPlayers.add(pong.sender.getUuid());
            }
        }

        if (pong == null || !pong.pingedPlayers.contains(player.getUuid()))
            player.sendMessage(message, typeKey);
    }
}
