package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.basiqueevangelist.pingspam.utils.NameLogic;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AliasCommand {
    private static final SimpleCommandExceptionType ALIAS_EXISTS = new SimpleCommandExceptionType(new LiteralText("You already have that alias!"));
    private static final DynamicCommandExceptionType ALIAS_EXISTS_OTHER = new DynamicCommandExceptionType(x ->
        ((ServerPlayerEntity) x).getDisplayName().shallowCopy().append(new LiteralText(" already has that alias!")));
    private static final SimpleCommandExceptionType NO_SUCH_ALIAS = new SimpleCommandExceptionType(new LiteralText("You don't have that alias!"));
    private static final DynamicCommandExceptionType NO_SUCH_ALIAS_OTHER = new DynamicCommandExceptionType(x ->
        ((ServerPlayerEntity) x).getDisplayName().shallowCopy().append(new LiteralText(" doesn't have that alias!")));
    private static final SimpleCommandExceptionType ALIAS_COLLISION = new SimpleCommandExceptionType(new LiteralText("Alias collides with other player's alias!"));
    private static final SimpleCommandExceptionType INVALID_ALIAS = new SimpleCommandExceptionType(new LiteralText("Invalid alias!"));
    private static final SimpleCommandExceptionType TOO_MANY_ALIASES = new SimpleCommandExceptionType(new LiteralText("Too many aliases! (maximum is 10)"));
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
                        .then(argument("player", EntityArgumentType.player())
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
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        for (String alias : PlayerUtils.getAliasesOf(player)) {
            builder.suggest(SuggestionsUtils.wrapString(alias));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOwnAliases(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        for (String alias : PlayerUtils.getAliasesOf(player)) {
            builder.suggest(SuggestionsUtils.wrapString(alias));
        }
        return builder.buildFuture();
    }

    private static int removePlayerAlias(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String alias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        if (!ALIAS_PATTERN.asPredicate().test(alias))
            throw INVALID_ALIAS.create();
        if (aliases.stream().noneMatch(x -> x.equalsIgnoreCase(alias)))
            throw NO_SUCH_ALIAS_OTHER.create(player);
        aliases.removeIf(x -> x.equalsIgnoreCase(alias));
        if (!NameLogic.isValidName(src.getServer().getPlayerManager(), alias, false))
            ServerNetworkLogic.removePossibleName(src.getServer().getPlayerManager(), alias);
        src.sendFeedback(
            new LiteralText("Removed alias ")
                .formatted(Formatting.RED)
                .append(new LiteralText('"' + alias + '"')
                    .formatted(Formatting.YELLOW))
                .append(new LiteralText(" from "))
                .append(new LiteralText(player.getEntityName())
                    .formatted(Formatting.AQUA))
                .append(new LiteralText(".")), true);
        return 0;
    }

    private static int addPlayerAlias(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newAlias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        if (!ALIAS_PATTERN.asPredicate().test(newAlias))
            throw INVALID_ALIAS.create();
        if (aliases.stream().anyMatch(x -> x.equalsIgnoreCase(newAlias)))
            throw ALIAS_EXISTS_OTHER.create(player);
        if (NameLogic.isValidName(src.getServer().getPlayerManager(), newAlias, false))
            throw ALIAS_COLLISION.create();
        if (aliases.size() >= ALIAS_LIMIT && !Permissions.check(src, "pingspam.bypass.aliaslimit", 2))
            throw TOO_MANY_ALIASES.create();
        aliases.add(newAlias);
        ServerNetworkLogic.addPossibleName(src.getServer().getPlayerManager(), newAlias);
        src.sendFeedback(
            new LiteralText("Added alias ")
                .formatted(Formatting.GREEN)
                .append(new LiteralText('"' + newAlias + '"')
                    .formatted(Formatting.YELLOW))
                .append(new LiteralText(" to "))
                .append(new LiteralText(player.getEntityName())
                    .formatted(Formatting.AQUA))
                .append(new LiteralText(".")), true);
        return 0;
    }

    private static int listPlayerAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        headerBuilder.append(" has ");
        headerBuilder.append(aliases.size());
        headerBuilder.append(" alias");
        if (aliases.size() != 1)
            headerBuilder.append("es");
        if (aliases.size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (String alias : aliases) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(alias);
            }
        } else {
            headerBuilder.append('.');
        }
        src.sendFeedback(new LiteralText(player.getEntityName())
            .formatted(Formatting.AQUA)
            .append(
                new LiteralText(headerBuilder.toString())
                    .formatted(Formatting.GREEN)
            )
            .append(
                new LiteralText(contentBuilder.toString())
                    .formatted(Formatting.YELLOW)
            ), false);

        return 0;
    }

    private static int removeAlias(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String alias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = src.getPlayer();
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        if (!ALIAS_PATTERN.asPredicate().test(alias))
            throw INVALID_ALIAS.create();
        if (aliases.stream().noneMatch(x -> x.equalsIgnoreCase(alias)))
            throw NO_SUCH_ALIAS.create();
        aliases.removeIf(x -> x.equalsIgnoreCase(alias));
        if (!NameLogic.isValidName(src.getServer().getPlayerManager(), alias, false))
            ServerNetworkLogic.removePossibleName(src.getServer().getPlayerManager(), alias);
        src.sendFeedback(new LiteralText("Removed alias ")
            .formatted(Formatting.RED)
            .append(new LiteralText('"' + alias + '"')
                .formatted(Formatting.YELLOW))
            .append(new LiteralText(".")), false);
        return 0;
    }

    private static int addAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newAlias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = src.getPlayer();
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        if (!ALIAS_PATTERN.asPredicate().test(newAlias))
            throw INVALID_ALIAS.create();
        if (aliases.stream().anyMatch(x -> x.equalsIgnoreCase(newAlias)))
            throw ALIAS_EXISTS.create();
        if (NameLogic.isValidName(src.getServer().getPlayerManager(), newAlias, false))
            throw ALIAS_COLLISION.create();
        if (aliases.size() >= ALIAS_LIMIT && !Permissions.check(src, "pingspam.bypass.aliaslimit", 2))
            throw TOO_MANY_ALIASES.create();
        aliases.add(newAlias);
        ServerNetworkLogic.addPossibleName(src.getServer().getPlayerManager(), newAlias);
        src.sendFeedback(new LiteralText("Added alias ")
            .formatted(Formatting.GREEN)
            .append(new LiteralText('"' + newAlias + '"')
                .formatted(Formatting.YELLOW))
            .append(new LiteralText(".")), false);
        return 0;
    }

    private static int listAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        headerBuilder.append("You have ");
        headerBuilder.append(aliases.size());
        headerBuilder.append(" alias");
        if (aliases.size() != 1)
            headerBuilder.append("es");
        if (aliases.size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (String alias : aliases) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(alias);
            }
        } else {
            headerBuilder.append('.');
        }
        src.sendFeedback(
            new LiteralText(headerBuilder.toString())
                .formatted(Formatting.GREEN)
                .append(new LiteralText(contentBuilder.toString())
                    .formatted(Formatting.YELLOW)), false);

        return 0;
    }


}
