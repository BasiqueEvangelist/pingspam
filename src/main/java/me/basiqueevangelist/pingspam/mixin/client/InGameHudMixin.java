package me.basiqueevangelist.pingspam.mixin.client;

import me.basiqueevangelist.pingspam.client.logic.ClientPingLogic;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.network.MessageType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "addChatMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private void onChatMessage(MessageType type, Text message, UUID senderUuid, CallbackInfo ci) {
        ClientPingLogic.processMessage(type, message, senderUuid);
    }
}
