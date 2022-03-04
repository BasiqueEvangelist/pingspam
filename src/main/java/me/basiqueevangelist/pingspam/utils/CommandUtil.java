package me.basiqueevangelist.pingspam.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.pingspam.data.PingspamPersistentState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CommandUtil {
    private static final SimpleCommandExceptionType TOO_MANY_PLAYERS = new SimpleCommandExceptionType(new LiteralText("Can't mention many players at once!"));

    private CommandUtil() {

    }

    public static GameProfile getOnePlayer(CommandContext<ServerCommandSource> ctx, String argName) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(ctx, "player");

        if (profiles.size() > 1)
            throw TOO_MANY_PLAYERS.create();

        return profiles.iterator().next();
    }

    public static CompletableFuture<Suggestions> suggestPlayersExceptSelf(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        var playerNames = new HashSet<>(List.of(ctx.getSource().getServer().getPlayerNames()));
        var pingspamData = PingspamPersistentState.getFrom(ctx.getSource().getServer());

        for (var id : pingspamData.getPlayerMap().keySet()) {
            playerNames.add(NameUtil.getNameFromUUID(id));
        }

        playerNames.remove(ctx.getSource().getPlayer().getEntityName());

        return CommandSource.suggestMatching(playerNames, builder);
    }
}
