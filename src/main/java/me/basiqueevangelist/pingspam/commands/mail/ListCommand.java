package me.basiqueevangelist.pingspam.commands.mail;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.MailMessage;
import me.basiqueevangelist.pingspam.data.PechkinPlayerData;
import me.basiqueevangelist.pingspam.utils.TimeUtils;
import me.basiqueevangelist.pingspam.utils.CommandUtil;
import me.basiqueevangelist.pingspam.utils.NameUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ListCommand {
    private ListCommand() {

    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mail")
            .then(literal("list")
                .executes(ListCommand::list)
                .then(argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtil::suggestPlayersExceptSelf)
                    .requires(Permissions.require("pechkin.list.other", 2))
                    .executes(ListCommand::listOther)))
            .executes(ListCommand::list));
    }

    public static int listOther(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getId(), PingSpam.PECHKIN_PLAYER_DATA);
        Text playerName = Text.literal(player.getName())
            .formatted(Formatting.AQUA);

        MutableText complete = Text.literal("")
            .append(playerName)
            .append(" has " + data.messages().size() + " message" + (data.messages().size() != 1 ? "s" : "") + " stored:");

        for (var message : data.messages()) {
            complete.append(writeMessageDesc(message, playerName, "/mail internal delete_list_other " + player.getName() + " "));
        }

        src.sendFeedback(() -> complete, false);

        return 1;
    }

    public static int list(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PECHKIN_PLAYER_DATA);

        MutableText complete = Text.literal("You have " + data.messages().size() + " message" + (data.messages().size() != 1 ? "s" : "") + " stored:");

        for (var message : data.messages()) {
            complete.append(writeMessageDesc(message, player.getDisplayName(), "/mail internal delete_list "));
        }

        src.sendFeedback(() -> complete, false);

        return 1;
    }

    private static Text writeMessageDesc(MailMessage msg, Text playerName, String deleteCmdPrefix) {
        return Text.literal("\n[")
            .append(Text.literal("✘")
                .formatted(Formatting.RED)
                .styled(x -> x
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, deleteCmdPrefix + msg.messageId()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Delete message")))))
            .append(" ")
            .append(Text.literal("i")
                .formatted(Formatting.BLUE)
                .styled(x -> x.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (
                    Text.literal("Sent ")
                        .append(TimeUtils.formatTime(msg.sentAt()))
                        .append(" ago\nUUID: " + msg.messageId())
                )))))
            .append("] ")
            .append(Text.literal(NameUtil.getNameFromUUID(msg.sender())).formatted(Formatting.AQUA))
            .append(Text.literal(" -> ").formatted(Formatting.WHITE))
            .append(playerName.copy().formatted(Formatting.AQUA))
            .append(Text.literal(": ").formatted(Formatting.WHITE))
            .append(msg.contents());
    }
}
