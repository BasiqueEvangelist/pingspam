package me.basiqueevangelist.pingspam.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import me.basiqueevangelist.pingspam.utils.CommandUtil;
import me.basiqueevangelist.pingspam.utils.NameUtil;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PingIgnoreCommand {
    private static final SimpleCommandExceptionType PLAYER_NOT_IGNORED = new SimpleCommandExceptionType(Text.literal("You are not ignoring this player!"));
    private static final SimpleCommandExceptionType PLAYER_ALREADY_IGNORED = new SimpleCommandExceptionType(Text.literal("You have already ignored this player!"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingspam")
                .then(literal("ignore")
                    .then(literal("add")
                        .then(argument("player", GameProfileArgumentType.gameProfile())
                            .suggests(CommandUtil::suggestPlayersExceptSelf)
                            .executes(PingIgnoreCommand::addIgnoredPlayer)))
                    .then(literal("remove")
                        .then(argument("player", GameProfileArgumentType.gameProfile())
                            .suggests(PingIgnoreCommand::suggestIgnoredPlayers)
                            .executes(PingIgnoreCommand::removeIgnoredPlayer)))
                    .then(literal("list")
                        .executes(PingIgnoreCommand::listIgnoredPlayers)))
        );
    }

    private static CompletableFuture<Suggestions> suggestIgnoredPlayers(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        for (UUID ignoredId : data.ignoredPlayers()) {
            builder.suggest(SuggestionsUtils.wrapString(NameUtil.getNameFromUUID(ignoredId)));
        }

        return builder.buildFuture();
    }

    private static int addIgnoredPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile offender = CommandUtil.getOnePlayer(ctx, "player");
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        if (data.ignoredPlayers().contains(offender.getId())) {
            throw PLAYER_ALREADY_IGNORED.create();
        }

        data.ignoredPlayers().add(offender.getId());

        src.sendFeedback(Text.literal("You are now ignoring ")
            .formatted(Formatting.GREEN)
            .append(Text.literal(offender.getName())
                .formatted(Formatting.AQUA))
            .append(Text.literal(".")), false);

        return 0;
    }

    private static int removeIgnoredPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile pardonee = CommandUtil.getOnePlayer(ctx, "player");
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        if (!data.ignoredPlayers().contains(pardonee.getId()))
            throw PLAYER_NOT_IGNORED.create();

        data.ignoredPlayers().remove(pardonee.getId());

        src.sendFeedback(Text.literal("You are no longer ignoring ")
            .formatted(Formatting.GREEN)
            .append(Text.literal(pardonee.getName())
                .formatted(Formatting.AQUA))
            .append("."), false);

        return 0;
    }

    private static int listIgnoredPlayers(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        if (data.ignoredPlayers().isEmpty()) {
            src.sendFeedback(Text.literal("You are not ignoring any players.")
                .formatted(Formatting.GREEN), false);
            return 0;
        }

        StringBuilder contentBuilder = new StringBuilder();
        for (UUID ignoredPlayerUuid : data.ignoredPlayers()) {
            contentBuilder.append("\n - ").append(NameUtil.getNameFromUUID(ignoredPlayerUuid));
        }

        src.sendFeedback(Text.literal("You are ignoring " + data.ignoredPlayers().size() + " player(s):")
            .formatted(Formatting.GREEN)
            .append(Text.literal(contentBuilder.toString())
                .formatted(Formatting.AQUA)), false);

        return 0;
    }
}
