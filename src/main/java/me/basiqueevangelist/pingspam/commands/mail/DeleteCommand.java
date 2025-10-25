package me.basiqueevangelist.pingspam.commands.mail;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.PechkinPlayerData;
import me.basiqueevangelist.pingspam.hack.StateTracker;
import me.basiqueevangelist.pingspam.logic.PingspamPermissions;
import me.basiqueevangelist.pingspam.utils.CommandUtil;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class DeleteCommand {
    private static final SimpleCommandExceptionType MESSAGE_DOESNT_EXIST = new SimpleCommandExceptionType(Text.literal("No such message"));

    private DeleteCommand() {

    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mail")
            .then(literal("internal")
                .requires(x -> !StateTracker.IS_IN_COMMAND_TREE_CREATION)
                .then(literal("delete_list")
                    .then(argument("message", UuidArgumentType.uuid())
                        .executes(DeleteCommand::deleteList)))
                .then(literal("delete_list_other")
                    .requires(PingspamPermissions::listOtherMail)
                    .requires(PingspamPermissions::deleteOtherMail)
                    .then(argument("player", GameProfileArgumentType.gameProfile())
                        .then(argument("message", UuidArgumentType.uuid())
                            .executes(DeleteCommand::deleteListOther))))
                .then(literal("delete_silent")
                    .then(argument("message", UuidArgumentType.uuid())
                        .executes(DeleteCommand::deleteSilent)))));
    }

    private static int deleteSilent(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        UUID messageId = UuidArgumentType.getUuid(ctx, "message");
        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PECHKIN_PLAYER_DATA);

        data.messages().removeIf(x -> x.messageId().equals(messageId));

        return 1;
    }

    private static int deleteList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        UUID messageId = UuidArgumentType.getUuid(ctx, "message");
        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PECHKIN_PLAYER_DATA);

        if (!data.messages().removeIf(x -> x.messageId().equals(messageId))) {
            throw MESSAGE_DOESNT_EXIST.create();
        }

        src.sendFeedback(() -> Text.literal("\n\n"), false);

        // Resend the list, since this command will only be invoked via list anyway 🚎
        ListCommand.list(ctx);

        return 1;
    }

    private static int deleteListOther(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        UUID messageId = UuidArgumentType.getUuid(ctx, "message");
        PlayerConfigEntry player = CommandUtil.getOnePlayer(ctx, "player");
        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.id(), PingSpam.PECHKIN_PLAYER_DATA);

        if (!data.messages().removeIf(x -> x.messageId().equals(messageId))) {
            throw MESSAGE_DOESNT_EXIST.create();
        }

        src.sendFeedback(() -> Text.literal("\n\n"), false);

        // Resend the list, since this command will only be invoked via list anyway 🚎
        ListCommand.listOther(ctx);

        return 1;
    }
}
