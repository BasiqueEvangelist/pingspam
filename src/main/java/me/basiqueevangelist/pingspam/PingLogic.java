package me.basiqueevangelist.pingspam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;


public final class PingLogic {
    private PingLogic() {

    }

    public static void pingOfflinePlayer(UUID playerUuid, Text pingMsg) {
        CompoundTag tag = OfflinePlayerCache.INSTANCE.reloadFor(playerUuid);
        if (tag.contains("UnreadPings")) {
            ListTag pingsTag = tag.getList("UnreadPings", 8);
            while (pingsTag.size() >= 100)
                pingsTag.remove(0);
            pingsTag.add(StringTag.of(Text.Serializer.toJson(pingMsg)));
        }
        OfflinePlayerCache.INSTANCE.saveFor(playerUuid, tag);
    }

    public static void pingOnlinePlayer(ServerPlayerEntity player, Text message, MessageType type, UUID senderUUID) {
        player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.5F, 1.0F);

        List<Text> unreadPings = PlayerUtils.getUnreadPingsFor(player);
        while (unreadPings.size() >= 100) {
            unreadPings.remove(0);
        }
        unreadPings.add(message);

        Text pingMessage = message.shallowCopy().formatted(Formatting.AQUA);
        player.networkHandler.sendPacket(new GameMessageS2CPacket(pingMessage, type, senderUUID));
    }

    public static void sendPingError(ServerPlayerEntity sender, String text) {
        if (PingSpam.CONFIG.getConfig().sendPingErrors)
            sender.sendSystemMessage(new LiteralText(text).formatted(Formatting.RED), Util.NIL_UUID);
    }
}
