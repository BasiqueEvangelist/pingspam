package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.GroupChatLogic;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "handleDecoratedMessage", at = @At("HEAD"), cancellable = true)
    private void trySendToChat(SignedMessage message, CallbackInfo ci) {
        String currentChat = DataStore.getFor(server).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA).currentChat();

        if (currentChat != null) {
            GroupChatLogic.sendIn(player, currentChat, message);
            ci.cancel();
        }
    }
}
