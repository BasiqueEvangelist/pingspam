package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.logic.GroupChatLogic;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler {
    @Shadow public ServerPlayerEntity player;

    public ServerPlayNetworkHandlerMixin(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Inject(method = "handleDecoratedMessage", at = @At("HEAD"), cancellable = true)
    private void trySendToChat(SignedMessage message, CallbackInfo ci) {
        if (!PingSpam.CONFIG.getConfig().groupChatsEnabled) return;

        String currentChat = DataStore.getFor(server).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA).currentChat();

        if (currentChat != null) {
            GroupChatLogic.sendIn(player, currentChat, message);
            ci.cancel();
        }
    }
}
