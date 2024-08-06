package me.basiqueevangelist.pechkin.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pechkin.Pechkin;
import me.basiqueevangelist.pechkin.data.PechkinPlayerData;
import me.basiqueevangelist.pechkin.util.CommandUtil;
import me.basiqueevangelist.pingspam.utils.NameUtil;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class IgnoreCommand {
    private static SimpleCommandExceptionType SELF_IGNORE = new SimpleCommandExceptionType(Text.literal("Can't (un)ignore yourself!"));
    private static SimpleCommandExceptionType ALREADY_IGNORED = new SimpleCommandExceptionType(Text.literal("You are already ignoring that player."));
    private static SimpleCommandExceptionType NOT_IGNORED = new SimpleCommandExceptionType(Text.literal("You aren't ignoring that player!"));

    private IgnoreCommand() {

    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mail")
            .then(literal("ignore")
                .then(literal("add")
                    .then(argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(IgnoreCommand::ignoreAddSuggest)
                        .executes(IgnoreCommand::ignoreAdd)))
                .then(literal("remove")
                    .then(argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(IgnoreCommand::ignoreRemoveSuggest)
                        .executes(IgnoreCommand::ignoreRemove)))
                .then(literal("list")
                    .executes(IgnoreCommand::ignoreList))));
    }

    private static int ignoreAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        GameProfile offender = CommandUtil.getOnePlayer(ctx, "player");

        if (offender.getId().equals(player.getUuid()))
            throw SELF_IGNORE.create();

        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), Pechkin.PLAYER_DATA);

        if (data.ignoredPlayers().contains(offender.getId()))
            throw ALREADY_IGNORED.create();

        data.ignoredPlayers().add(offender.getId());

        src.sendFeedback(() -> Text.literal("Ignoring any further messages from ")
            .formatted(Formatting.GREEN)
            .append(Text.literal(NameUtil.getNameFromUUID(offender.getId())).formatted(Formatting.AQUA))
            .append("."), false);

        return 1;
    }

    private static int ignoreRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        GameProfile offender = CommandUtil.getOnePlayer(ctx, "player");

        if (offender.getId().equals(player.getUuid()))
            throw SELF_IGNORE.create();

        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), Pechkin.PLAYER_DATA);

        if (!data.ignoredPlayers().remove(offender.getId()))
            throw NOT_IGNORED.create();

        src.sendFeedback(() -> Text.literal("Stopped ignoring messages from ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(NameUtil.getNameFromUUID(offender.getId())).formatted(Formatting.AQUA))
            .append("."), false);

        return 1;
    }

    private static CompletableFuture<Suggestions> ignoreAddSuggest(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), Pechkin.PLAYER_DATA);

        for (var playerId : data.lastCorrespondents()) {
            builder.suggest(NameUtil.getNameFromUUID(playerId));
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> ignoreRemoveSuggest(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), Pechkin.PLAYER_DATA);

        for (var playerId : data.ignoredPlayers()) {
            builder.suggest(NameUtil.getNameFromUUID(playerId));
        }

        return builder.buildFuture();
    }

    private static int ignoreList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();

        PechkinPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), Pechkin.PLAYER_DATA);

        MutableText playersBuilder = Text.literal("");
        boolean isFirst = true;

        for (UUID ignoredPlayer : data.ignoredPlayers()) {
            if (!isFirst)
                playersBuilder.append(", ");
            isFirst = false;
            playersBuilder.append(Text.literal(NameUtil.getNameFromUUID(ignoredPlayer)).formatted(Formatting.AQUA));
        }

        if (isFirst)
            src.sendFeedback(() -> Text.literal("You aren't ignoring messages from anybody.").formatted(Formatting.GREEN), false);
        else
            src.sendFeedback(() -> Text.literal("You are ignoring messages from ")
                .formatted(Formatting.GREEN)
                .append(playersBuilder)
                .append("."), false);

        return 1;
    }
}
