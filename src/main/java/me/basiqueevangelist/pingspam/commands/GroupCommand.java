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
import me.basiqueevangelist.pingspam.data.PingspamPersistentState;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.basiqueevangelist.pingspam.utils.CommandUtil;
import me.basiqueevangelist.pingspam.utils.NameLogic;
import me.basiqueevangelist.pingspam.utils.NameUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GroupCommand {
    private static final DynamicCommandExceptionType IN_GROUP_OTHER = new DynamicCommandExceptionType(x ->
        new LiteralText(NameUtil.getNameFromUUID(((GameProfile) x).getId())).append(new LiteralText(" is already in that group!")));
    private static final DynamicCommandExceptionType NOT_IN_GROUP_OTHER = new DynamicCommandExceptionType(x ->
        new LiteralText(NameUtil.getNameFromUUID(((GameProfile) x).getId())).append(new LiteralText(" isn't in that group!")));
    private static final SimpleCommandExceptionType NAME_COLLISION = new SimpleCommandExceptionType(new LiteralText("Group name collides with other player's alias!"));
    private static final SimpleCommandExceptionType INVALID_GROUPNAME = new SimpleCommandExceptionType(new LiteralText("Invalid group name!"));
    private static final Pattern GROUPNAME_PATTERN = Pattern.compile("^[\\w0-9_]{2,16}$", Pattern.UNICODE_CHARACTER_CLASS);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingspam")
                .then(literal("group")
                    .then(literal("list")
                        .executes(GroupCommand::listOwnGroups))
                    .then(literal("player")
                        .then(argument("player", GameProfileArgumentType.gameProfile())
                            .then(literal("add")
                                .requires(x -> Permissions.check(x, "pingspam.group.player.add", 2))
                                .then(argument("groupname", StringArgumentType.string())
                                    .executes(GroupCommand::addPlayerGroup)))
                            .then(literal("remove")
                                .requires(x -> Permissions.check(x, "pingspam.group.player.remove", 2))
                                .then(argument("groupname", StringArgumentType.string())
                                    .suggests(GroupCommand::suggestPlayerGroups)
                                    .executes(GroupCommand::removePlayerGroup)))
                            .then(literal("list")
                                .executes(GroupCommand::listPlayerGroups)))))
        );
    }

    private static int listPlayerGroups(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPlayerData data = PingspamPersistentState.getFrom(ctx.getSource().getServer()).getFor(player.getId());

        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        headerBuilder.append(" is in ");
        headerBuilder.append(data.groups().size());
        headerBuilder.append(" ping group");

        if (data.groups().size() != 1)
            headerBuilder.append("s");

        if (data.groups().size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (String group : data.groups()) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(group);
            }
        } else {
            headerBuilder.append('.');
        }

        src.sendFeedback(new LiteralText(NameUtil.getNameFromUUID(player.getId()))
            .formatted(Formatting.AQUA)
            .append(new LiteralText(headerBuilder.toString())
                .formatted(Formatting.GREEN))
            .append(new LiteralText(contentBuilder.toString())
                .formatted(Formatting.YELLOW)), false);

        return 0;
    }

    private static int removePlayerGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String group = StringArgumentType.getString(ctx, "groupname");
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPersistentState state = PingspamPersistentState.getFrom(ctx.getSource().getServer());
        PingspamPlayerData data = state.getFor(player.getId());

        if (!GROUPNAME_PATTERN.asPredicate().test(group))
            throw INVALID_GROUPNAME.create();

        if (!data.groups().contains(group))
            throw NOT_IN_GROUP_OTHER.create(player);

        state.removePlayerFromGroup(group, player.getId());

        if (!NameLogic.isValidName(src.getServer(), group, false))
            ServerNetworkLogic.removePossibleName(src.getServer().getPlayerManager(), group);

        src.sendFeedback(
            new LiteralText("Removed player ")
                .formatted(Formatting.GREEN)
                .append(new LiteralText(NameUtil.getNameFromUUID(player.getId()))
                    .formatted(Formatting.AQUA))
                .append(" from group ")
                .append(new LiteralText(group)
                    .formatted(Formatting.YELLOW))
                .append(new LiteralText(".")), true);

        return 0;
    }

    private static CompletableFuture<Suggestions> suggestPlayerGroups(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPlayerData data = PingspamPersistentState.getFrom(ctx.getSource().getServer()).getFor(player.getId());

        for (String group : data.groups()) {
            builder.suggest(SuggestionsUtils.wrapString(group));
        }

        return builder.buildFuture();
    }

    private static int addPlayerGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newGroup = StringArgumentType.getString(ctx, "groupname");
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPersistentState state = PingspamPersistentState.getFrom(ctx.getSource().getServer());
        PingspamPlayerData data = state.getFor(player.getId());

        if (!GROUPNAME_PATTERN.asPredicate().test(newGroup))
            throw INVALID_GROUPNAME.create();

        if (data.groups().contains(newGroup))
            throw IN_GROUP_OTHER.create(player);

        if (NameLogic.isValidName(src.getServer(), newGroup, true))
            throw NAME_COLLISION.create();

        state.addPlayerToGroup(newGroup, player.getId());
        ServerNetworkLogic.addPossibleName(src.getServer().getPlayerManager(), newGroup);

        src.sendFeedback(
            new LiteralText("Added player ")
                .formatted(Formatting.GREEN)
                .append(new LiteralText(NameUtil.getNameFromUUID(player.getId()))
                    .formatted(Formatting.AQUA))
                .append(" to group ")
                .append(new LiteralText(newGroup)
                    .formatted(Formatting.YELLOW)
                .append(".")), true);

        return 0;
    }

    private static int listOwnGroups(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        PingspamPlayerData data = PingspamPersistentState.getFrom(ctx.getSource().getServer()).getFor(player.getUuid());

        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        headerBuilder.append("You are in ");
        headerBuilder.append(data.groups().size());
        headerBuilder.append(" ping group");

        if (data.groups().size() != 1)
            headerBuilder.append("s");

        if (data.groups().size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (String pingGroup : data.groups()) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(pingGroup);
            }
        } else {
            headerBuilder.append('.');
        }

        src.sendFeedback(new LiteralText(headerBuilder.toString())
            .formatted(Formatting.GREEN)
            .append(new LiteralText(contentBuilder.toString())
                .formatted(Formatting.YELLOW)), false);

        return 0;
    }
}
