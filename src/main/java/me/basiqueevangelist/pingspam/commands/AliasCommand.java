package me.basiqueevangelist.pingspam.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.basiqueevangelist.pingspam.utils.CommandUtil;
import me.basiqueevangelist.pingspam.utils.NameLogic;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AliasCommand {
    private static final SimpleCommandExceptionType ALIAS_EXISTS = new SimpleCommandExceptionType(Text.literal("You already have that alias!"));
    private static final DynamicCommandExceptionType ALIAS_EXISTS_OTHER = new DynamicCommandExceptionType(x ->
        Text.literal(((GameProfile) x).getName()).append(Text.literal(" already has that alias!")));
    private static final SimpleCommandExceptionType NO_SUCH_ALIAS = new SimpleCommandExceptionType(Text.literal("You don't have that alias!"));
    private static final DynamicCommandExceptionType NO_SUCH_ALIAS_OTHER = new DynamicCommandExceptionType(x ->
        Text.literal(((GameProfile) x).getName()).append(" doesn't have that alias!"));
    private static final SimpleCommandExceptionType ALIAS_COLLISION = new SimpleCommandExceptionType(Text.literal("That is already a valid name!"));
    private static final SimpleCommandExceptionType INVALID_ALIAS = new SimpleCommandExceptionType(Text.literal("Invalid alias!"));
    private static final SimpleCommandExceptionType TOO_MANY_ALIASES = new SimpleCommandExceptionType(Text.literal("Too many aliases! (maximum is 10)"));
    public static final int ALIAS_LIMIT = 10;
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[\\w0-9_]{2,16}$", Pattern.UNICODE_CHARACTER_CLASS);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingspam")
                .then(literal("alias")
                    .then(literal("list")
                        .executes(AliasCommand::listAliases))
                    .then(literal("add")
                        .requires(x -> Permissions.check(x, "pingspam.alias.own.add", true))
                        .then(argument("alias", StringArgumentType.string())
                            .executes(AliasCommand::addAliases)))
                    .then(literal("remove")
                        .requires(x -> Permissions.check(x, "pingspam.alias.own.remove", true))
                        .then(argument("alias", StringArgumentType.string())
                            .executes(AliasCommand::removeAlias)
                            .suggests(AliasCommand::suggestOwnAliases)))
                    .then(literal("player")
                        .then(argument("player", GameProfileArgumentType.gameProfile())
                            .suggests(CommandUtil::suggestPlayersExceptSelf)
                            .then(literal("list")
                                .executes(AliasCommand::listPlayerAliases))
                            .then(literal("add")
                                .requires(x -> Permissions.check(x, "pingspam.alias.player.add", 2))
                                .then(argument("alias", StringArgumentType.string())
                                    .executes(AliasCommand::addPlayerAlias)))
                            .then(literal("remove")
                                .requires(x -> Permissions.check(x, "pingspam.alias.player.remove", 2))
                                .then(argument("alias", StringArgumentType.string())
                                    .executes(AliasCommand::removePlayerAlias)
                                    .suggests(AliasCommand::suggestPlayerAliases))))))
        );
    }

    private static CompletableFuture<Suggestions> suggestPlayerAliases(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getId(), PingSpam.PLAYER_DATA);

        for (String alias : data.aliases()) {
            builder.suggest(SuggestionsUtils.wrapString(alias));
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnAliases(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        for (String alias : data.aliases()) {
            builder.suggest(SuggestionsUtils.wrapString(alias));
        }

        return builder.buildFuture();
    }

    private static int removePlayerAlias(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String alias = StringArgumentType.getString(ctx, "alias");
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getId(), PingSpam.PLAYER_DATA);

        if (!ALIAS_PATTERN.asPredicate().test(alias))
            throw INVALID_ALIAS.create();

        if (!data.aliases().contains(alias))
            throw NO_SUCH_ALIAS_OTHER.create(player);

        data.aliases().remove(alias);

        if (!NameLogic.isValidName(src.getServer(), alias, false))
            ServerNetworkLogic.removePossibleName(src.getServer().getPlayerManager(), alias);

        src.sendFeedback(
            () -> Text.literal("Removed alias ")
                .formatted(Formatting.GREEN)
                .append(Text.literal('"' + alias + '"')
                    .formatted(Formatting.YELLOW))
                .append(Text.literal(" from "))
                .append(Text.literal(player.getName())
                    .formatted(Formatting.AQUA))
                .append(Text.literal(".")), true);

        return 0;
    }

    private static int addPlayerAlias(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newAlias = StringArgumentType.getString(ctx, "alias");
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getId(), PingSpam.PLAYER_DATA);

        if (!ALIAS_PATTERN.asPredicate().test(newAlias))
            throw INVALID_ALIAS.create();

        if (data.aliases().contains(newAlias))
            throw ALIAS_EXISTS_OTHER.create(player);

        if (NameLogic.isValidName(src.getServer(), newAlias, false))
            throw ALIAS_COLLISION.create();

        if (data.aliases().size() >= ALIAS_LIMIT && !Permissions.check(src, "pingspam.bypass.aliaslimit", 2))
            throw TOO_MANY_ALIASES.create();

        data.aliases().add(newAlias);
        ServerNetworkLogic.addPossibleName(src.getServer().getPlayerManager(), newAlias);

        src.sendFeedback(
            () -> Text.literal("Added alias ")
                .formatted(Formatting.GREEN)
                .append(Text.literal('"' + newAlias + '"')
                    .formatted(Formatting.YELLOW))
                .append(Text.literal(" to "))
                .append(Text.literal(player.getName())
                    .formatted(Formatting.AQUA))
                .append(Text.literal(".")), true);

        return 0;
    }

    private static int listPlayerAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getId(), PingSpam.PLAYER_DATA);

        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        headerBuilder.append(" has ");
        headerBuilder.append(data.aliases().size());
        headerBuilder.append(" alias");

        if (data.aliases().size() != 1)
            headerBuilder.append("es");

        if (data.aliases().size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (String alias : data.aliases()) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(alias);
            }
        } else {
            headerBuilder.append('.');
        }

        src.sendFeedback(() -> Text.literal(player.getName())
            .formatted(Formatting.AQUA)
            .append(
                Text.literal(headerBuilder.toString())
                    .formatted(Formatting.GREEN)
            )
            .append(
                Text.literal(contentBuilder.toString())
                    .formatted(Formatting.YELLOW)
            ), false);

        return 0;
    }

    private static int removeAlias(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String alias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        if (!ALIAS_PATTERN.asPredicate().test(alias))
            throw INVALID_ALIAS.create();

        if (!data.aliases().contains(alias))
            throw NO_SUCH_ALIAS.create();

        data.aliases().remove(alias);

        if (!NameLogic.isValidName(src.getServer(), alias, false))
            ServerNetworkLogic.removePossibleName(src.getServer().getPlayerManager(), alias);

        src.sendFeedback(() -> Text.literal("Removed alias ")
            .formatted(Formatting.GREEN)
            .append(Text.literal('"' + alias + '"')
                .formatted(Formatting.YELLOW))
            .append(Text.literal(".")), false);

        return 0;
    }

    private static int addAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newAlias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        if (!ALIAS_PATTERN.asPredicate().test(newAlias))
            throw INVALID_ALIAS.create();

        if (data.aliases().contains(newAlias))
            throw ALIAS_EXISTS.create();

        if (NameLogic.isValidName(src.getServer(), newAlias, false))
            throw ALIAS_COLLISION.create();

        if (data.aliases().size() >= ALIAS_LIMIT && !Permissions.check(src, "pingspam.bypass.aliaslimit", 2))
            throw TOO_MANY_ALIASES.create();

        data.aliases().add(newAlias);
        ServerNetworkLogic.addPossibleName(src.getServer().getPlayerManager(), newAlias);

        src.sendFeedback(() -> Text.literal("Added alias ")
            .formatted(Formatting.GREEN)
            .append(Text.literal('"' + newAlias + '"')
                .formatted(Formatting.YELLOW))
            .append(Text.literal(".")), false);

        return 0;
    }

    private static int listAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();

        headerBuilder.append("You have ");
        headerBuilder.append(data.aliases().size());
        headerBuilder.append(" alias");

        if (data.aliases().size() != 1)
            headerBuilder.append("es");

        if (data.aliases().size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (String alias : data.aliases()) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(alias);
            }
        } else {
            headerBuilder.append('.');
        }

        src.sendFeedback(
            () -> Text.literal(headerBuilder.toString())
                .formatted(Formatting.GREEN)
                .append(Text.literal(contentBuilder.toString())
                    .formatted(Formatting.YELLOW)), false);

        return 0;
    }
}
