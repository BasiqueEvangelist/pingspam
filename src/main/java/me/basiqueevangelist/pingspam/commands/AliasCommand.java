package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.pingspam.PlayerUtils;
import me.basiqueevangelist.pingspam.AliasLogic;
import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.List;
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
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingspam")
                .then(literal("alias")
                    .then(literal("list")
                        .executes(AliasCommand::listAliases))
                    .then(literal("add")
                        .requires(x -> Permissions.check(x, "pingspam.alias.own.add", true))
                        .then(argument("alias", StringArgumentType.word())
                            .executes(AliasCommand::addAliases)))
                    .then(literal("remove")
                        .requires(x -> Permissions.check(x, "pingspam.alias.own.remove", true))
                        .then(argument("alias", StringArgumentType.word())
                            .executes(AliasCommand::removeAlias)))
                    .then(literal("player")
                        .then(argument("player", EntityArgumentType.player())
                            .then(literal("list")
                                .executes(AliasCommand::listPlayerAliases))
                            .then(literal("add")
                                .requires(x -> Permissions.check(x, "pingspam.alias.player.add", 2))
                                .then(argument("alias", StringArgumentType.word())
                                    .executes(AliasCommand::addPlayerAlias)))
                            .then(literal("remove")
                                .requires(x -> Permissions.check(x, "pingspam.alias.player.remove", 2))
                                .then(argument("alias", StringArgumentType.word())
                                    .executes(AliasCommand::removePlayerAlias))))))
        );
    }

    private static int removePlayerAlias(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String alias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        if (!ALIAS_PATTERN.asPredicate().test(alias))
            throw INVALID_ALIAS.create();
        if (!aliases.contains(alias))
            throw NO_SUCH_ALIAS_OTHER.create(player);
        aliases.remove(alias);
        if (!PlayerUtils.anyPlayer(src.getMinecraftServer().getPlayerManager(), alias))
            ServerNetworkLogic.removePossibleName(src.getMinecraftServer().getPlayerManager(), alias);
        src.sendFeedback(
            new LiteralText("Removed alias \"" + alias + "\" from ")
                .append(player.getDisplayName())
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
        if (aliases.contains(newAlias))
            throw ALIAS_EXISTS_OTHER.create(player);
        if (AliasLogic.checkForCollision(src.getMinecraftServer().getPlayerManager(), newAlias))
            throw ALIAS_COLLISION.create();
        if (aliases.size() >= ALIAS_LIMIT && !Permissions.check(src, "pingspam.bypass.aliaslimit", 2))
            throw TOO_MANY_ALIASES.create();
        aliases.add(newAlias);
        ServerNetworkLogic.addPossibleName(src.getMinecraftServer().getPlayerManager(), newAlias);
        src.sendFeedback(
            new LiteralText("Added alias \"" + newAlias + "\" to ")
                .append(player.getDisplayName())
                .append(new LiteralText(".")), true);
        return 0;
    }

    private static int listPlayerAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        StringBuilder sb = new StringBuilder();
        sb.append(" has ");
        sb.append(aliases.size());
        sb.append(" alias");
        if (aliases.size() != 1)
            sb.append("es");
        if (aliases.size() > 0) {
            sb.append(": ");
            boolean isFirst = true;
            for (String alias : aliases) {
                if (!isFirst)
                    sb.append(", ");
                isFirst = false;
                sb.append(alias);
            }
        } else {
            sb.append('.');
        }
        src.sendFeedback(player.getDisplayName().shallowCopy().append(new LiteralText(sb.toString())), false);

        return 0;
    }

    private static int removeAlias(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String alias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = src.getPlayer();
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        if (!ALIAS_PATTERN.asPredicate().test(alias))
            throw INVALID_ALIAS.create();
        if (!aliases.contains(alias))
            throw NO_SUCH_ALIAS.create();
        aliases.remove(alias);
        if (!PlayerUtils.anyPlayer(src.getMinecraftServer().getPlayerManager(), alias))
            ServerNetworkLogic.removePossibleName(src.getMinecraftServer().getPlayerManager(), alias);
        src.sendFeedback(new LiteralText("Removed alias \"" + alias + "\"."), false);
        return 0;
    }

    private static int addAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newAlias = StringArgumentType.getString(ctx, "alias");
        ServerPlayerEntity player = src.getPlayer();
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        if (!ALIAS_PATTERN.asPredicate().test(newAlias))
            throw INVALID_ALIAS.create();
        if (aliases.contains(newAlias))
            throw ALIAS_EXISTS.create();
        if (AliasLogic.checkForCollision(src.getMinecraftServer().getPlayerManager(), newAlias))
            throw ALIAS_COLLISION.create();
        if (aliases.size() >= ALIAS_LIMIT && !Permissions.check(src, "pingspam.bypass.aliaslimit", 2))
            throw TOO_MANY_ALIASES.create();
        aliases.add(newAlias);
        ServerNetworkLogic.addPossibleName(src.getMinecraftServer().getPlayerManager(), newAlias);
        src.sendFeedback(new LiteralText("Added alias \"" + newAlias + "\"."), false);
        return 0;
    }

    private static int listAliases(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        List<String> aliases = PlayerUtils.getAliasesOf(player);
        StringBuilder sb = new StringBuilder();
        sb.append("You have ");
        sb.append(aliases.size());
        sb.append(" alias");
        if (aliases.size() != 1)
            sb.append("es");
        if (aliases.size() > 0) {
            sb.append(": ");
            boolean isFirst = true;
            for (String alias : aliases) {
                if (!isFirst)
                    sb.append(", ");
                isFirst = false;
                sb.append(alias);
            }
        } else {
            sb.append('.');
        }
        src.sendFeedback(new LiteralText(sb.toString()), false);

        return 0;
    }


}
