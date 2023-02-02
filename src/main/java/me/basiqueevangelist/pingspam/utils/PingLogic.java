package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.onedatastore.api.PlayerDataEntry;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.PingspamGroupData;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class PingLogic {
    public static final Pattern PING_PATTERN = Pattern.compile("@([\\w0-9_]{2,})", Pattern.UNICODE_CHARACTER_CLASS);

    private PingLogic() {

    }

    public static class ProcessedPing {
        public List<UUID> pingedPlayers = new ArrayList<>();
        public MinecraftServer server;
        public boolean pingSucceeded = false;
        public ServerPlayerEntity sender;
        public Predicate<UUID> playerPredicate;
    }

    public static ProcessedPing processPings(MinecraftServer server, Text messageContent, Text message, UUID senderUuid,
                                             @Nullable Predicate<UUID> playerPredicate) {
        String contents = messageContent.getString();
        ServerPlayerEntity sender = server.getPlayerManager().getPlayer(senderUuid);
        Matcher matcher = PING_PATTERN.matcher(contents);
        ProcessedPing result = new ProcessedPing();
        result.sender = sender;
        result.server = server;
        result.playerPredicate = playerPredicate == null ? unused -> true : playerPredicate;
        if (PingSpam.CONFIG.getConfig().processPingsFromUnknownPlayers || sender != null) {
            while (matcher.find()) {
                String username = matcher.group(1);
                processMention(result, username, message);
            }
        }
        return result;
    }

    private static void processMention(ProcessedPing result, String mention, Text message) {
        switch (mention) {
            case "everyone":
                if (result.sender == null || Permissions.check(result.sender, "pingspam.ping.everyone", 2)) {
                    for (UUID playerId : PlayerUtils.getAllPlayers(result.server)) {
                        pingPlayer(result, playerId, message);
                    }
                    result.pingSucceeded = true;
                } else {
                    PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping @everyone!");
                }
                break;
            case "online":
                if (result.sender == null || Permissions.check(result.sender, "pingspam.ping.online", 2)) {
                    for (ServerPlayerEntity player : result.server.getPlayerManager().getPlayerList()) {
                        pingPlayer(result, player.getUuid(), message);
                    }
                    result.pingSucceeded = true;
                } else {
                    PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping @online!");
                }
                break;
            case "offline":
                if (result.sender == null || Permissions.check(result.sender, "pingspam.ping.offline", 2)) {
                    for (PlayerDataEntry entry : DataStore.getFor(result.server).players()) {
                        if (result.server.getPlayerManager().getPlayer(entry.playerId()) != null) continue;

                        pingPlayer(result, entry.playerId(), message);
                    }
                    result.pingSucceeded = true;
                } else {
                    PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping @offline!");
                }
                break;
            default:
                PingspamGroupData pingGroup = DataStore.getFor(result.server).get(PingSpam.GLOBAL_DATA).groups().get(mention);
                if (pingGroup != null && pingGroup.isPingable()) {
                    if (result.sender == null || Permissions.check(result.sender, "pingspam.ping.group", true)) {
                        for (UUID playerId : pingGroup.members()) {
                            pingPlayer(result, playerId, message);
                        }

                        result.pingSucceeded = true;
                    } else {
                        PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping group @" + mention + "!");
                    }
                    return;
                }

                UUID foundPlayerId = PlayerUtils.tryFindPlayer(result.server, mention);

                if (foundPlayerId == null) {
                    if (result.sender != null)
                        PingLogic.sendPingError(result.sender, "No such player: " + mention + "!");
                    return;
                }

                if (!result.playerPredicate.test(foundPlayerId)) {
                    if (result.sender != null)
                        PingLogic.sendPingError(result.sender, "@" + mention + " is unreachable in this context");
                    return;
                }

                PingspamPlayerData foundData = DataStore.getFor(result.server).getPlayer(foundPlayerId, PingSpam.PLAYER_DATA);

                if (result.sender != null && !Permissions.check(result.sender, "pingspam.bypass.ignore", 2)) {
                    if (foundData.ignoredPlayers().contains(result.sender.getUuid())) {
                        PingLogic.sendPingError(result.sender, NameUtil.getNameFromUUID(foundPlayerId) + " has ignored you, they won't receive your ping.");
                        break;
                    }
                }

                if (result.sender != null && !Permissions.check(result.sender, "pingspam.ping.player", true)) {
                    PingLogic.sendPingError(result.sender, "You do not have enough permissions to ping @" + mention + "!");
                    return;
                }

                PingLogic.pingPlayer(result, foundPlayerId, message);
                result.pingSucceeded = true;
        }
    }

    public static void pingPlayer(ProcessedPing ping, UUID playerUuid, Text pingMsg) {
        if (ping.pingedPlayers.contains(playerUuid)) return;
        if (!ping.playerPredicate.test(playerUuid)) return;

        ping.pingedPlayers.add(playerUuid);
        sendNotification(ping.server, playerUuid, pingMsg);
    }

    public static void sendNotification(MinecraftServer server, UUID playerId, Text pingMsg) {
        PingspamPlayerData data = DataStore.getFor(server).getPlayer(playerId, PingSpam.PLAYER_DATA);

        data.addPing(pingMsg);

        ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(playerId);
        if (onlinePlayer != null) {
            SoundEvent pingSound = data.pingSound();

            if (pingSound != null) {
                onlinePlayer.playSound(pingSound, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public static void sendPingError(ServerPlayerEntity sender, String text) {
        if (PingSpam.CONFIG.getConfig().sendPingErrors)
            sender.sendMessage(Text.literal(text).formatted(Formatting.RED), false);
    }
}
