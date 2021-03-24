package me.basiqueevangelist.pingspam.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.pingspam.utils.OfflinePlayerCache;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

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
                                    builder.suggest(getNameFromUuid(ignoredUuid));
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
        GameProfile ignorePlayer = getProfile(ctx);
        ServerPlayerEntity player = src.getPlayer();
        List<UUID> ignoredPlayers = PlayerUtils.getIgnoredPlayersOf(player);

        if (ignoredPlayers.contains(ignorePlayer.getId())) {
            throw PLAYER_ALREADY_IGNORED.create();
        }

        ignoredPlayers.add(ignorePlayer.getId());

        src.sendFeedback(new LiteralText("You are now ignoring ")
            .formatted(Formatting.GREEN)
            .append(new LiteralText(ignorePlayer.getName())
                .formatted(Formatting.AQUA))
            .append(new LiteralText(".")), false);

        return 0;
    }

    private static int removeIgnoredPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile removePlayer = getProfile(ctx);
        ServerPlayerEntity player = src.getPlayer();
        List<UUID> ignoredPlayers = PlayerUtils.getIgnoredPlayersOf(player);

        if (!ignoredPlayers.contains(removePlayer.getId())) {
            throw PLAYER_NOT_IGNORED.create();
        }

        ignoredPlayers.remove(removePlayer.getId());

        src.sendFeedback(new LiteralText("You are no longer ignoring ")
            .formatted(Formatting.RED)
            .append(new LiteralText(removePlayer.getName())
                .formatted(Formatting.AQUA))
            .append("."), false);

        return 0;
    }

    private static int listIgnoredPlayers(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        List<UUID> ignoredPlayers = PlayerUtils.getIgnoredPlayersOf(player);

        if (ignoredPlayers.isEmpty()) {
            src.sendFeedback(new LiteralText("You are not ignoring any players."), false);
            return 0;
        }

        StringBuilder contentBuilder = new StringBuilder();
        int count = 0;
        for (UUID ignoredPlayerUuid : ignoredPlayers) {
            contentBuilder.append("\n - ").append(getNameFromUuid(ignoredPlayerUuid));
            count++;
        }

        src.sendFeedback(new LiteralText("You are ignoring " + count + " player(s):")
            .formatted(Formatting.GREEN)
            .append(new LiteralText(contentBuilder.toString())
                .formatted(Formatting.YELLOW)), false);

        return 0;
    }

    private static GameProfile getProfile(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        GameProfile profile = GameProfileArgumentType.getProfileArgument(ctx, "player").iterator().next();

        if (!profile.isComplete()) {
            return OfflinePlayerCache.INSTANCE.getServer().getSessionService().fillProfileProperties(profile, false);
        }

        return profile;
    }

    private static String getNameFromUuid(UUID uuid) {
        GameProfile userCacheProfile = OfflinePlayerCache.INSTANCE.getServer().getUserCache().getByUuid(uuid);

        if (userCacheProfile != null && userCacheProfile.isComplete()) {
            return userCacheProfile.getName();
        } else {
            return OfflinePlayerCache.INSTANCE.getServer().getSessionService().fillProfileProperties(new GameProfile(uuid, null), false).getName();
        }
    }
}
