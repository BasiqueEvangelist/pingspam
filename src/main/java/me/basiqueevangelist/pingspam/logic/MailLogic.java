package me.basiqueevangelist.pingspam.logic;

import me.basiqueevangelist.pingspam.data.MailMessage;
import me.basiqueevangelist.pingspam.utils.NameUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public final class MailLogic {
    private MailLogic() {

    }

    public static void notifyMailReceived(MinecraftServer server, UUID receiverId, ServerPlayerEntity sender, MailMessage message) {
        Text fancyMessage = Text.literal("")
            .append(sender.getDisplayName().copy().formatted(Formatting.AQUA))
            .append(Text.literal(" -> ").formatted(Formatting.WHITE))
            .append(Text.literal(NameUtil.getNameFromUUID(receiverId)).formatted(Formatting.AQUA))
            .append(Text.literal(": ").formatted(Formatting.WHITE))
            .append(message.contents())
            .append(" [")
            .append(Text.literal("✔")
                .formatted(Formatting.GREEN)
                .styled(x -> x
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Acknowledge and delete message")))
                    .withClickEvent(new ClickEvent.RunCommand("/mail internal delete_silent " + message.messageId()))))
            .append("]");

        ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(receiverId);

        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(fancyMessage);
        }

        PingLogic.sendNotification(server, receiverId, fancyMessage);
    }

    public static void notifyMailSent(UUID recipient, ServerPlayerEntity sender, MailMessage message) {
        sender.sendMessage(Text.literal("")
            .append(sender.getDisplayName().copy().formatted(Formatting.AQUA))
            .append(Text.literal(" -> ").formatted(Formatting.WHITE))
            .append(Text.literal(NameUtil.getNameFromUUID(recipient)).formatted(Formatting.AQUA))
            .append(Text.literal(": ").formatted(Formatting.WHITE))
            .append(message.contents()));
    }
}
