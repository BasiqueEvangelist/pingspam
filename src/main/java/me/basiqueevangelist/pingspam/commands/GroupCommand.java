package me.basiqueevangelist.pingspam.commands;


import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import me.basiqueevangelist.pingspam.utils.NameUtil;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GroupCommand {
    private static final DynamicCommandExceptionType IN_GROUP_OTHER = new DynamicCommandExceptionType(x ->
        Text.literal(((GameProfile) x).getName()).append(Text.literal(" is already in that group")));
    private static final DynamicCommandExceptionType NOT_IN_GROUP_OTHER = new DynamicCommandExceptionType(x ->
        Text.literal(((GameProfile) x).getName()).append(Text.literal(" isn't in that group")));
    private static final SimpleCommandExceptionType NAME_COLLISION = new SimpleCommandExceptionType(Text.literal("That name is already taken"));
    public static final SimpleCommandExceptionType NO_SUCH_GROUP = new SimpleCommandExceptionType(Text.literal("No such group"));
    private static final SimpleCommandExceptionType INVALID_GROUPNAME = new SimpleCommandExceptionType(Text.literal("Invalid group name"));
    private static final Pattern GROUPNAME_PATTERN = Pattern.compile("^[\\w0-9_]{2,16}$", Pattern.UNICODE_CHARACTER_CLASS);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingspam")
                .then(literal("group")
                    .then(argument("group", StringArgumentType.string())
                        .suggests(GroupCommand::suggestGroups)
                        .then(literal("list")
                            .executes(GroupCommand::listPlayersInGroup))
                        .then(literal("add")
                            .requires(Permissions.require("pingspam.group.player.add", 2))
                            .then(argument("player", GameProfileArgumentType.gameProfile())
                                .suggests(CommandUtil::suggestPlayers)
                                .executes(GroupCommand::addPlayerToGroup)))
                        .then(literal("remove")
                            .requires(Permissions.require("pingspam.group.player.add", 2))
                            .then(argument("player", GameProfileArgumentType.gameProfile())
                                .suggests(CommandUtil::suggestPlayers)
                                .executes(GroupCommand::removePlayerFromGroup)))
                        .then(literal("pingable")
                            .requires(Permissions.require("pingspam.group.configure", 2))
                            .then(argument("value", BoolArgumentType.bool())
                                .executes(GroupCommand::configurePingable)))
                        .then(literal("haschat")
                            .requires(Permissions.require("pingspam.group.configure", 2))
                            .then(argument("value", BoolArgumentType.bool())
                                .executes(GroupCommand::configureHasChat)))
                    ))
        );
    }

    private static int configurePingable(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String groupName = StringArgumentType.getString(ctx, "group");
        var group = DataStore.getFor(src.getServer()).get(PingSpam.GLOBAL_DATA).groups().get(groupName);

        if (group == null)
            throw NO_SUCH_GROUP.create();

        boolean value = BoolArgumentType.getBool(ctx, "value");
        boolean old = group.isPingable();
        group.isPingable(value);

        if (old && !value) {
            if (!NameLogic.isValidName(src.getServer(), groupName, false))
                ServerNetworkLogic.removePossibleName(src.getServer().getPlayerManager(), groupName);
        } else if (!old && value) {
            ServerNetworkLogic.addPossibleName(src.getServer().getPlayerManager(), groupName);
        }

        src.sendFeedback(Text.literal((value ? "Enabled" : "Disabled") + " pinging of ")
            .formatted(Formatting.GREEN)
            .append(Text.literal("@" + groupName)
                    .formatted(Formatting.YELLOW))
            .append("."), true);

        return 1;
    }

    private static int configureHasChat(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String groupName = StringArgumentType.getString(ctx, "group");
        var group = DataStore.getFor(src.getServer()).get(PingSpam.GLOBAL_DATA).groups().get(groupName);

        if (group == null)
            throw NO_SUCH_GROUP.create();

        boolean value = BoolArgumentType.getBool(ctx, "value");
        group.hasChat(value);

        src.sendFeedback(Text.literal((value ? "Enabled" : "Disabled") + " group chat for ")
            .formatted(Formatting.GREEN)
            .append(Text.literal("@" + groupName)
                .formatted(Formatting.YELLOW))
            .append("."), true);

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestGroups(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        ServerCommandSource src = ctx.getSource();

        for (String groupName : DataStore.getFor(src.getServer()).get(PingSpam.GLOBAL_DATA).groups().keySet()) {
            builder.suggest(SuggestionsUtils.wrapString(groupName));
        }

        return builder.buildFuture();
    }

    private static int listPlayersInGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String groupName = StringArgumentType.getString(ctx, "group");
        var group = DataStore.getFor(src.getServer()).get(PingSpam.GLOBAL_DATA).groups().get(groupName);

        if (group == null)
            throw NO_SUCH_GROUP.create();

        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        headerBuilder.append(" has ");
        headerBuilder.append(group.members().size());
        headerBuilder.append(" player");

        if (group.members().size() != 1)
            headerBuilder.append("s");

        if (group.members().size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (UUID playerId : group.members()) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(NameUtil.getNameFromUUID(playerId));
            }
        } else {
            headerBuilder.append('.');
        }

        src.sendFeedback(Text.literal("Group ")
            .formatted(Formatting.GREEN)
            .append(Text.literal("@" + groupName)
                .formatted(Formatting.YELLOW))
            .append(headerBuilder.toString())
            .append(Text.literal(contentBuilder.toString())
                .formatted(Formatting.AQUA)), false);

        return 0;
    }

    private static int removePlayerFromGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        DataStore store = DataStore.getFor(src.getServer());
        String group = StringArgumentType.getString(ctx, "group");
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPlayerData data = store.getPlayer(player.getId(), PingSpam.PLAYER_DATA);

        if (!GROUPNAME_PATTERN.asPredicate().test(group))
            throw INVALID_GROUPNAME.create();

        if (!data.groups().contains(group))
            throw NOT_IN_GROUP_OTHER.create(player);

        store.get(PingSpam.GLOBAL_DATA).removePlayerFromGroup(group, player.getId());

        if (!NameLogic.isValidName(src.getServer(), group, false))
            ServerNetworkLogic.removePossibleName(src.getServer().getPlayerManager(), group);

        src.sendFeedback(
            Text.literal("Removed player ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(player.getName())
                    .formatted(Formatting.AQUA))
                .append(" from group ")
                .append(Text.literal(group)
                    .formatted(Formatting.YELLOW))
                .append(Text.literal(".")), true);

        return 0;
    }

    private static int addPlayerToGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var src = ctx.getSource();
        DataStore store = DataStore.getFor(src.getServer());
        String group = StringArgumentType.getString(ctx, "group");
        GameProfile player = CommandUtil.getOnePlayer(ctx, "player");
        PingspamPlayerData data = store.getPlayer(player.getId(), PingSpam.PLAYER_DATA);

        if (!GROUPNAME_PATTERN.asPredicate().test(group))
            throw INVALID_GROUPNAME.create();

        if (data.groups().contains(group))
            throw IN_GROUP_OTHER.create(player);

        if (NameLogic.isValidName(src.getServer(), group, true))
            throw NAME_COLLISION.create();

        store.get(PingSpam.GLOBAL_DATA).addPlayerToGroup(group, player.getId());
        ServerNetworkLogic.addPossibleName(src.getServer().getPlayerManager(), group);

        src.sendFeedback(
            Text.literal("Added player ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(player.getName())
                    .formatted(Formatting.AQUA))
                .append(" to group ")
                .append(Text.literal("@" + group)
                    .formatted(Formatting.YELLOW)
                .append(".")), true);

        return 0;
    }
}
