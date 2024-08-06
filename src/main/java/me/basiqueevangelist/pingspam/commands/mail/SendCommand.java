package me.basiqueevangelist.pingspam.commands.mail;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.MailMessage;
import me.basiqueevangelist.pingspam.data.PechkinPlayerData;
import me.basiqueevangelist.pingspam.logic.IgnoreLogic;
import me.basiqueevangelist.pingspam.logic.MailLogic;
import me.basiqueevangelist.pingspam.utils.CommandUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class SendCommand {
    private static final SimpleCommandExceptionType IGNORED = new SimpleCommandExceptionType(Text.literal("That player has ignored you."));
    private static final SimpleCommandExceptionType RATELIMIT = new SimpleCommandExceptionType(Text.literal("You are being rate limited."));
    private static final SimpleCommandExceptionType NO_CORRESPONDENT = new SimpleCommandExceptionType(Text.literal("No correspondents recorded yet"));

    private SendCommand() {

    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mail")
            .then(literal("send")
                .then(argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtil::suggestPlayers)
                    .then(argument("message", MessageArgumentType.message())
                        .requires(Permissions.require("pechkin.send", true))
                        .executes(SendCommand::send)))));

        dispatcher.register(literal("r")
            .then(argument("message", MessageArgumentType.message())
                .requires(Permissions.require("pechkin.send", true))
                .executes(SendCommand::reply)));
    }

    private static int send(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity sender = src.getPlayerOrThrow();
        GameProfile recipient = CommandUtil.getOnePlayer(ctx, "player");
        Text message = MessageArgumentType.getMessage(ctx, "message");

        sendMessage(src, sender, recipient.getId(), message);

        return 1;
    }

    private static int reply(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity sender = src.getPlayerOrThrow();
        PechkinPlayerData senderData = DataStore.getFor(src.getServer()).getPlayer(sender.getUuid(), PingSpam.PECHKIN_PLAYER_DATA);
        Text message = MessageArgumentType.getMessage(ctx, "message");

        if (senderData.lastCorrespondents().size() <= 0)
            throw NO_CORRESPONDENT.create();

        UUID recipientId = senderData.lastCorrespondents().get(0);

        sendMessage(src, sender, recipientId, message);

        return 1;
    }

    private static void sendMessage(ServerCommandSource src, ServerPlayerEntity sender, UUID recipientId, Text message) throws CommandSyntaxException {
        PechkinPlayerData senderData = DataStore.getFor(src.getServer()).getPlayer(sender.getUuid(), PingSpam.PECHKIN_PLAYER_DATA);
        PechkinPlayerData recipientData = DataStore.getFor(src.getServer()).getPlayer(recipientId, PingSpam.PECHKIN_PLAYER_DATA);

        IgnoreLogic.throwIfIgnored(sender, recipientId);

        if (!Permissions.check(sender, "pechkin.bypass.cooldown", 2)) {
            int sendCost = PingSpam.CONFIG.getConfig().mail.sendCost;

            if (!senderData.leakyBucket().hasEnoughFor(sendCost))
                throw RATELIMIT.create();

            senderData.leakyBucket().addTime(sendCost);
        }

        if (!recipientId.equals(sender.getUuid()))
            recipientData.addCorrespondent(sender.getUuid());

        MailMessage mail = new MailMessage(message, sender.getUuid(), UUID.randomUUID(), Instant.now());
        recipientData.addMessage(mail);
        MailLogic.notifyMailSent(recipientId, sender, mail);

        if (sender.getUuid().equals(recipientId)) return;

        MailLogic.notifyMailReceived(src.getServer(), recipientId, sender, mail);
    }
}
