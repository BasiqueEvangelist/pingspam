package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.basiqueevangelist.pingspam.utils.MessageTypeTransformer;
import me.basiqueevangelist.pingspam.utils.PingLogic;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Decoration;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
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
import java.util.function.Predicate;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Shadow @Final private MinecraftServer server;
    @Unique private PingLogic.ProcessedPing pong;

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void onPlayerConnected(ClientConnection conn, ServerPlayerEntity player, CallbackInfo ci) {
        ServerNetworkLogic.addPossibleName((PlayerManager)(Object) this, player.getGameProfile().getName());
        ServerNetworkLogic.sendServerAnnouncement(player, conn);
    }

    @Inject(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;logChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageType$Parameters;Ljava/lang/String;)V", shift = At.Shift.AFTER))
    public void processPingSigned(SignedMessage message, Predicate<ServerPlayerEntity> shouldSendFiltered, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci) {
        Decoration rule = params.type().chat();
        UUID uuid = sender == null ? Util.NIL_UUID : sender.getUuid();

        if (rule != null) {
            pong = PingLogic.processPings(server, message.getContent(), rule.apply(message.getContent(), params), uuid);
        }
    }

    @Inject(method = "broadcast*", at = @At("RETURN"))
    private void deletePing(CallbackInfo ci) {
        pong = null;
    }

    @Redirect(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendChatMessage(Lnet/minecraft/network/message/SentMessage;ZLnet/minecraft/network/message/MessageType$Parameters;)V"))
    private void sendMessageSigned(ServerPlayerEntity player, SentMessage message, boolean filterMaskEnabled, MessageType.Parameters params) {
        if (pong != null && pong.pingSucceeded) {
            var typeRegistry = server.getRegistryManager().get(RegistryKeys.MESSAGE_TYPE);
            var oldKey = typeRegistry.getKey(params.type()).orElseThrow();
            RegistryKey<MessageType> newKey = null;

            if (pong.pingedPlayers.contains(player.getUuid())) {
                newKey = RegistryKey.of(RegistryKeys.MESSAGE_TYPE, MessageTypeTransformer.wrapPinged(oldKey.getValue()));
            } else if (pong.sender == player) {
                newKey = RegistryKey.of(RegistryKeys.MESSAGE_TYPE, MessageTypeTransformer.wrapPingSuccessful(oldKey.getValue()));
                pong.pingedPlayers.add(pong.sender.getUuid());
            }

            if (newKey != null)
                player.sendChatMessage(message, filterMaskEnabled, new MessageType.Parameters(typeRegistry.get(newKey), params.name(), params.targetName()));

        }

        if (pong == null || !pong.pingedPlayers.contains(player.getUuid()))
            player.sendChatMessage(message, filterMaskEnabled, params);
    }

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;sendMessage(Lnet/minecraft/text/Text;)V", shift = At.Shift.AFTER))
    private void processPing(Text message, Function<ServerPlayerEntity, Text> playerMessageFactory, boolean overlay, CallbackInfo ci) {
        if (overlay) return;

        pong = PingLogic.processPings(server, message, message, Util.NIL_UUID);
    }

    @Redirect(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendMessageToClient(Lnet/minecraft/text/Text;Z)V"))
    private void sendMessage(ServerPlayerEntity player, Text message, boolean overlay) {
        if (!overlay && pong != null && pong.pingSucceeded) {
            if (pong.pingedPlayers.contains(player.getUuid())) {
                player.sendMessage(message.copy().formatted(Formatting.AQUA), false);
            } else if (pong.sender == player) {
                player.sendMessage(message.copy().formatted(Formatting.GOLD), false);
                pong.pingedPlayers.add(pong.sender.getUuid());
            }
        }

        if (pong == null || !pong.pingedPlayers.contains(player.getUuid()))
            player.sendMessage(message, overlay);
    }
}
