package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.nevseti.OfflineDataCache;
import me.basiqueevangelist.nevseti.OfflineNameCache;
import me.basiqueevangelist.nevseti.nbt.CompoundTagView;
import me.basiqueevangelist.pingspam.PingSpam;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class PingLogic {
    public static final Pattern PING_PATTERN = Pattern.compile("@([\\w0-9_]{2,})", Pattern.UNICODE_CHARACTER_CLASS);

    private PingLogic() {

    }

    public static class ProcessedPing {
        public PlayerList pingedPlayers = new PlayerList();
        public boolean pingSucceeded = false;
        public ServerPlayerEntity sender;
    }

    public static ProcessedPing processPings(PlayerManager manager, Text message, MessageType type, UUID senderUuid) {
        String contents = message.getString();
        ServerPlayerEntity sender = manager.getPlayer(senderUuid);
        Matcher matcher = PING_PATTERN.matcher(contents);
        ProcessedPing result = new ProcessedPing();
        result.sender = sender;
        if (PingSpam.CONFIG.getConfig().processPingsFromUnknownPlayers || sender != null) {
            while (matcher.find()) {
                String username = matcher.group(1);
                processMention(manager, result, username, message, type, senderUuid);
            }
        }
        return result;
    }

    private static void processMention(PlayerManager manager, ProcessedPing result, String mention, Text message, MessageType type, UUID senderUuid) {
        switch (mention) {
            case "everyone":
                if (result.sender == null || Permissions.check(result.sender, "pingspam.ping.everyone", 2)) {
                    pingAllIn(result, PlayerList.fromAllPlayers(manager), message, type, senderUuid);
                    result.pingSucceeded = true;
                } else {
                    PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping @everyone!");
                }
                break;
            case "online":
                if (result.sender == null || Permissions.check(result.sender, "pingspam.ping.online", 2)) {
                    pingAllIn(result, PlayerList.fromOnline(manager), message, type, senderUuid);
                    result.pingSucceeded = true;
                } else {
                    PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping @online!");
                }
                break;
            case "offline":
                if (result.sender == null || Permissions.check(result.sender, "pingspam.ping.offline", 2)) {
                    pingAllIn(result, PlayerList.fromOffline(manager), message, type, senderUuid);
                    result.pingSucceeded = true;
                } else {
                    PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping @offline!");
                }
                break;
            default:
                PlayerList pingGroup = PlayerUtils.queryPingGroup(manager, mention);
                if (!pingGroup.isEmpty()) {
                    if (result.sender == null || Permissions.check(result.sender, "pingspam.ping.group", true)) {
                        pingAllIn(result, pingGroup, message, type, senderUuid);
                        result.pingSucceeded = true;
                    } else {
                        PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping group @" + mention + "!");
                    }
                    return;
                }

                UUID foundPlayerUuid = PlayerUtils.tryFindPlayer(manager, mention);

                if (foundPlayerUuid == null) {
                    if (result.sender != null)
                        PingLogic.sendPingError(result.sender, "No such player: " + mention + "!");
                    return;
                }

                ServerPlayerEntity onlinePlayer = manager.getPlayer(foundPlayerUuid);
                if (result.sender != null && !Permissions.check(result.sender, "pingspam.bypass.ignore", 2)) {
                    if (onlinePlayer != null && PingLogic.isPlayerIgnoredBy(result.sender.getUuid(), onlinePlayer)) {
                        PingLogic.sendPingError(result.sender, onlinePlayer.getEntityName() + " has ignored you, they won't receive your ping.");
                        break;
                    } else if (onlinePlayer == null) {
                        CompoundTagView playerTag = OfflineDataCache.INSTANCE.get(foundPlayerUuid);
                        if (OfflineUtils.isPlayerIgnoredBy(playerTag, result.sender.getUuid())) {
                            PingLogic.sendPingError(result.sender, OfflineNameCache.INSTANCE.getNameFromUUID(foundPlayerUuid) + " has ignored you, they won't receive your ping.");
                            break;
                        }
                    }
                }

                if (result.sender != null && !Permissions.check(result.sender, "pingspam.ping.player", true)) {
                    PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping @" + mention + "!");
                    return;
                }

                if (onlinePlayer != null) {
                    if (!result.pingedPlayers.getOnlinePlayers().contains(onlinePlayer)) {
                        PingLogic.pingOnlinePlayer(onlinePlayer, message, type, senderUuid);
                        result.pingSucceeded = true;
                        result.pingedPlayers.add(onlinePlayer);
                    }
                    return;
                }

                if (!result.pingedPlayers.getOfflinePlayers().contains(foundPlayerUuid)) {
                    PingLogic.pingOfflinePlayer(foundPlayerUuid, message);
                    result.pingedPlayers.add(foundPlayerUuid);
                    result.pingSucceeded = true;
                }
        }
    }

    private static void pingAllIn(ProcessedPing result, PlayerList list, Text message, MessageType type, UUID senderUuid) {
        for (ServerPlayerEntity player : list.getOnlinePlayers()) {
            if (!result.pingedPlayers.getOnlinePlayers().contains(player)) {
                PingLogic.pingOnlinePlayer(player, message, type, senderUuid);
                result.pingedPlayers.add(player);
            }
        }
        for (UUID offlinePlayer : list.getOfflinePlayers()) {
            if (!result.pingedPlayers.getOfflinePlayers().contains(offlinePlayer)) {
                PingLogic.pingOfflinePlayer(offlinePlayer, message);
                result.pingedPlayers.add(offlinePlayer);
            }
        }
    }

    public static void pingOfflinePlayer(UUID playerUuid, Text pingMsg) {
        CompoundTag tag = OfflineDataCache.INSTANCE.reload(playerUuid).copy();
        if (tag.contains("UnreadPings")) {
            ListTag pingsTag = tag.getList("UnreadPings", 8);
            while (pingsTag.size() >= 100)
                pingsTag.remove(0);
            pingsTag.add(StringTag.of(Text.Serializer.toJson(pingMsg)));
        }
        OfflineDataCache.INSTANCE.save(playerUuid, tag);
    }

    public static void pingOnlinePlayer(ServerPlayerEntity player, Text message, MessageType type, UUID senderUUID) {
        SoundEvent pingSound = PlayerUtils.getPingSound(player);

        if (pingSound != null) {
            player.playSound(pingSound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

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

    public static boolean isPlayerIgnoredBy(UUID senderUuid, ServerPlayerEntity pingedPlayer) {
        return PlayerUtils.getIgnoredPlayersOf(pingedPlayer).contains(senderUuid);
    }
}
