package me.basiqueevangelist.pingspam.client.logic;

import me.basiqueevangelist.pingspam.client.PingSpamClient;
import me.basiqueevangelist.pingspam.client.network.PingSpamClientNetworking;
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
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class ClientPingLogic {
    private static final Pattern GENERIC_PATTERN = Pattern.compile("([\\w0-9_]{2,16})(\\s|$)", Pattern.UNICODE_CHARACTER_CLASS);

    private ClientPingLogic() {

    }

    public static void processMessage(MessageType type, Text message, UUID senderUuid) {
        if (type != MessageType.CHAT) return;

        if (!PingSpamClient.CONFIG.getConfig().localMentionScanning) return;
        if (PingSpamClientNetworking.getServerData() != null && !PingSpamClient.CONFIG.getConfig().alwaysScanMentions) return;

        String messageText = message.getString();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Matcher matcher = GENERIC_PATTERN.matcher(messageText);
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
