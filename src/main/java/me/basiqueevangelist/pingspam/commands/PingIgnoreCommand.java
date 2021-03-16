package me.basiqueevangelist.pingspam.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static me.basiqueevangelist.pingspam.PingSpam.SERVER;

public class PingIgnoreCommand {
    private static final SimpleCommandExceptionType PLAYER_NOT_IGNORED = new SimpleCommandExceptionType(new LiteralText("You are not ignoring this player!"));
    private static final SimpleCommandExceptionType PLAYER_ALREADY_IGNORED = new SimpleCommandExceptionType(new LiteralText("You have already ignored this player!"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("pingignore")
                        .then(literal("add")
                                .then(argument("player", GameProfileArgumentType.gameProfile())
                                        .executes(PingIgnoreCommand::addIgnoredPlayer)))
                        .then(literal("remove")
                                .then(argument("player", GameProfileArgumentType.gameProfile())
                                        .executes(PingIgnoreCommand::removeIgnoredPlayer)))
                        .then(literal("list")
                                .executes(PingIgnoreCommand::listIgnoredPlayers))
        );
    }

    private static int addIgnoredPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile ignorePlayer = getProfile(ctx);
        ServerPlayerEntityAccess player = (ServerPlayerEntityAccess) src.getPlayer();
        List<UUID> ignoredPlayers = player.pingspam$getIgnoredPlayers();

        if (ignoredPlayers.contains(ignorePlayer.getId())) {
            throw PLAYER_ALREADY_IGNORED.create();
        }

        player.pingspam$addIgnoredPlayer(ignorePlayer.getId());

        src.sendFeedback(new LiteralText("You are now ignoring " + ignorePlayer.getName() + "."), false);

        return 0;
    }

    private static int removeIgnoredPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile removePlayer = getProfile(ctx);
        ServerPlayerEntityAccess player = (ServerPlayerEntityAccess) src.getPlayer();
        List<UUID> ignoredPlayers = player.pingspam$getIgnoredPlayers();

        if (!ignoredPlayers.contains(removePlayer.getId())) {
            throw PLAYER_NOT_IGNORED.create();
        }

        player.pingspam$removeIgnoredPlayer(removePlayer.getId());

        src.sendFeedback(new LiteralText("You are no longer ignoring " + removePlayer.getName() + "."), false);

        return 0;
    }

    private static int listIgnoredPlayers(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntityAccess player = (ServerPlayerEntityAccess) src.getPlayer();
        List<UUID> ignoredPlayers = player.pingspam$getIgnoredPlayers();

        if (ignoredPlayers.isEmpty()) {
            src.sendFeedback(new LiteralText("You are not ignoring any players."), false);
            return 0;
        }

        StringBuilder ignoredPlayerMessage = new StringBuilder();
        int count = 0;
        for (UUID ignoredPlayerUuid : ignoredPlayers) {
            ignoredPlayerMessage.append("\n - ").append(getNameFromUuid(ignoredPlayerUuid));
            count++;
        }

        src.sendFeedback(new LiteralText("You are ignoring " + count + " player(s):" + ignoredPlayerMessage.toString()), false);

        return 0;
    }

    private static GameProfile getProfile(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        GameProfile profile = GameProfileArgumentType.getProfileArgument(ctx, "player").iterator().next();

        if (!profile.isComplete()) {
            return SERVER.getSessionService().fillProfileProperties(profile, false);
        }

        return profile;
    }

    private static String getNameFromUuid(UUID uuid) {
        GameProfile userCacheProfile = SERVER.getUserCache().getByUuid(uuid);

        if (userCacheProfile != null && userCacheProfile.isComplete()) {
            return userCacheProfile.getName();
        } else {
            return SERVER.getSessionService().fillProfileProperties(new GameProfile(uuid, null), false).getName();
        }
    }
}
