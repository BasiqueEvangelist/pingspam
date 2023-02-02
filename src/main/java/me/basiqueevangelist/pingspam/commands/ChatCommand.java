package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.utils.GroupChatLogic;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ChatCommand {
    private ChatCommand() {

    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingspam")
                .then(literal("chat")
                    .executes(ChatCommand::clearChat)
                    .then(argument("group", StringArgumentType.string())
                        .suggests(ChatCommand::suggestChatGroups)
                        .executes(ChatCommand::switchChat)
                        .then(argument("message", MessageArgumentType.message())
                            .executes(ChatCommand::sendToChat))))
        );
    }

    private static CompletableFuture<Suggestions> suggestChatGroups(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();

        for (var group : DataStore.getFor(src.getServer()).get(PingSpam.GLOBAL_DATA).groups().values()) {
            if (!group.hasChat() || !group.members().contains(ctx.getSource().getPlayerOrThrow().getUuid()))
                continue;

            builder.suggest(SuggestionsUtils.wrapString(group.name()));
        }

        return builder.buildFuture();
    }

    private static int switchChat(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String groupName = StringArgumentType.getString(ctx, "group");
        var group = DataStore.getFor(src.getServer()).get(PingSpam.GLOBAL_DATA).groups().get(groupName);

        if (group == null)
            throw GroupCommand.NO_SUCH_GROUP.create();

        if (!group.hasChat() || !group.members().contains(ctx.getSource().getPlayerOrThrow().getUuid()))
            throw GroupCommand.NO_SUCH_GROUP.create();

        DataStore.getFor(src.getServer())
            .getPlayer(src.getPlayerOrThrow().getUuid(), PingSpam.PLAYER_DATA)
            .currentChat(groupName);

        src.sendFeedback(Text.literal("Switched to the ")
            .formatted(Formatting.GREEN)
            .append(Text.literal("@" + groupName)
                .formatted(Formatting.YELLOW))
            .append(" chat."), false);

        return 1;
    }

    private static int clearChat(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        DataStore.getFor(src.getServer())
            .getPlayer(src.getPlayerOrThrow().getUuid(), PingSpam.PLAYER_DATA)
            .currentChat(null);

        src.sendFeedback(Text.literal("Switched to the global chat."), false);
        return 1;
    }

    private static int sendToChat(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String groupName = StringArgumentType.getString(ctx, "group");
        var group = DataStore.getFor(src.getServer()).get(PingSpam.GLOBAL_DATA).groups().get(groupName);

        if (group == null)
            throw GroupCommand.NO_SUCH_GROUP.create();

        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        if (!group.hasChat() || !group.members().contains(player.getUuid()))
            throw GroupCommand.NO_SUCH_GROUP.create();

        MessageArgumentType.getSignedMessage(ctx, "message", msg -> {
            GroupChatLogic.sendIn(player, groupName, msg);
        });

        return 1;
    }
}
