package me.basiqueevangelist.pingspam.client.logic;

import me.basiqueevangelist.pingspam.utils.PingLogic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;
import java.util.regex.Matcher;

@Environment(EnvType.CLIENT)
public final class ClientPingLogic {
    private ClientPingLogic() {

    }

    public static void processMessage(MessageType type, Text message, UUID senderUuid) {
        String messageText = message.getString();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Matcher matcher = PingLogic.PING_PATTERN.matcher(messageText);
        while (matcher.find()) {
            String username = matcher.group(1);

            if (username.equals(player.getEntityName())) {
                MutableText mutableMessage = (MutableText) message;
                mutableMessage.formatted(Formatting.AQUA);
                player.playSound(SoundEvents.BLOCK_BELL_USE, 1.0F, 1.0F);
            }
        }

    }
}
