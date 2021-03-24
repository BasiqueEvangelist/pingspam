package me.basiqueevangelist.pingspam.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.nevseti.OfflineNameCache;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PingIgnoreCommand {
    private static final SimpleCommandExceptionType PLAYER_NOT_IGNORED = new SimpleCommandExceptionType(new LiteralText("You are not ignoring this player!"));
    private static final SimpleCommandExceptionType PLAYER_ALREADY_IGNORED = new SimpleCommandExceptionType(new LiteralText("You have already ignored this player!"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingspam")
                .then(literal("ignore")
                    .then(literal("add")
                        .then(argument("player", GameProfileArgumentType.gameProfile())
                            .executes(PingIgnoreCommand::addIgnoredPlayer)))
                    .then(literal("remove")
                        .then(argument("player", GameProfileArgumentType.gameProfile())
                            .suggests((ctx, builder) -> {
                                for (UUID ignoredUuid : PlayerUtils.getIgnoredPlayersOf(ctx.getSource().getPlayer())) {
                                    builder.suggest(OfflineNameCache.INSTANCE.getNameFromUUID(ignoredUuid));
                                }
                                return builder.buildFuture();
                            })
                            .executes(PingIgnoreCommand::removeIgnoredPlayer)))
                    .then(literal("list")
                        .executes(PingIgnoreCommand::listIgnoredPlayers)))
        );
    }

    private static int addIgnoredPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        Collection<GameProfile> playersToIgnore = GameProfileArgumentType.getProfileArgument(ctx, "player");
        ServerPlayerEntity player = src.getPlayer();
        List<UUID> ignoredPlayers = PlayerUtils.getIgnoredPlayersOf(player);
        StringBuilder playersBuilder = new StringBuilder();

        boolean isFirst = true;
        for (GameProfile playerToIgnore : playersToIgnore) {
            if (!isFirst)
                playersBuilder.append(", ");
            isFirst = false;

            playersBuilder.append(playerToIgnore.getName());

            if (ignoredPlayers.contains(playerToIgnore.getId())) {
                throw PLAYER_ALREADY_IGNORED.create();
            }

            ignoredPlayers.add(playerToIgnore.getId());
        }

        src.sendFeedback(new LiteralText("You are now ignoring ")
            .formatted(Formatting.RED)
            .append(new LiteralText(playersBuilder.toString())
                .formatted(Formatting.AQUA))
            .append(new LiteralText(".")), false);

        return 0;
    }

    private static int removeIgnoredPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        Collection<GameProfile> removedPlayers = GameProfileArgumentType.getProfileArgument(ctx, "player");
        ServerPlayerEntity player = src.getPlayer();
        List<UUID> ignoredPlayers = PlayerUtils.getIgnoredPlayersOf(player);
        StringBuilder playersBuilder = new StringBuilder();

        boolean isFirst = true;
        for (GameProfile removedPlayer : removedPlayers) {
            if (!isFirst)
                playersBuilder.append(", ");
            isFirst = false;

            playersBuilder.append(removedPlayer.getName());

            if (!ignoredPlayers.contains(removedPlayer.getId())) {
                throw PLAYER_NOT_IGNORED.create();
            }

            ignoredPlayers.remove(removedPlayer.getId());
        }

        src.sendFeedback(new LiteralText("You are no longer ignoring ")
            .formatted(Formatting.GREEN)
            .append(new LiteralText(playersBuilder.toString())
                .formatted(Formatting.AQUA))
            .append("."), false);

        return 0;
    }

    private static int listIgnoredPlayers(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        List<UUID> ignoredPlayers = PlayerUtils.getIgnoredPlayersOf(player);

        if (ignoredPlayers.isEmpty()) {
            src.sendFeedback(new LiteralText("You are not ignoring any players.")
                .formatted(Formatting.GREEN), false);
            return 0;
        }

        StringBuilder contentBuilder = new StringBuilder();
        int count = 0;
        for (UUID ignoredPlayerUuid : ignoredPlayers) {
            contentBuilder.append("\n - ").append(OfflineNameCache.INSTANCE.getNameFromUUID(ignoredPlayerUuid));
            count++;
        }

        src.sendFeedback(new LiteralText("You are ignoring " + count + " player(s):")
            .formatted(Formatting.GREEN)
            .append(new LiteralText(contentBuilder.toString())
                .formatted(Formatting.AQUA)), false);

        return 0;
    }
}
