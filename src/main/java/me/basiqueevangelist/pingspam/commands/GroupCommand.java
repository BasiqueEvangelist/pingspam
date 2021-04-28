package me.basiqueevangelist.pingspam.commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.pingspam.integration.PingspamGroup;
import me.basiqueevangelist.pingspam.integration.PingspamGroupSource;
import me.basiqueevangelist.pingspam.network.ServerNetworkLogic;
import me.basiqueevangelist.pingspam.utils.NameLogic;
import me.basiqueevangelist.pingspam.utils.PlayerUtils;
import me.basiqueevangelist.regrouped.PlayerGroup;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GroupCommand {
    private static final DynamicCommandExceptionType IN_GROUP_OTHER = new DynamicCommandExceptionType(x ->
        ((ServerPlayerEntity) x).getDisplayName().shallowCopy().append(new LiteralText(" is already in that group!")));
    private static final DynamicCommandExceptionType NOT_IN_GROUP_OTHER = new DynamicCommandExceptionType(x ->
        ((ServerPlayerEntity) x).getDisplayName().shallowCopy().append(new LiteralText(" isn't in that group!")));
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
                        .then(argument("player", EntityArgumentType.player())
                            .then(literal("add")
                                .requires(x -> Permissions.check(x, "pingspam.group.player.add", 2))
                                .then(argument("groupname", StringArgumentType.string())
                                    .suggests(GroupCommand::suggestGroups)
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
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        List<PlayerGroup> groups = PingspamGroupSource.INSTANCE.getGroupsOf(player.getUuid());
        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        headerBuilder.append(" is in ");
        headerBuilder.append(groups.size());
        headerBuilder.append(" ping group");
        if (groups.size() != 1)
            headerBuilder.append("s");
        if (groups.size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (PlayerGroup group : groups) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(group.getName());
            }
        } else {
            headerBuilder.append('.');
        }
        src.sendFeedback(new LiteralText(player.getEntityName())
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
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        if (!GROUPNAME_PATTERN.asPredicate().test(group))
            throw INVALID_GROUPNAME.create();
        PingspamGroup groupInstance = PingspamGroupSource.INSTANCE.findGroupByName(group);
        if (groupInstance == null || !groupInstance.removeMember(player.getUuid()))
            throw NOT_IN_GROUP_OTHER.create(player);
        if (!NameLogic.isValidName(src.getMinecraftServer().getPlayerManager(), group, false))
            ServerNetworkLogic.removePossibleName(src.getMinecraftServer().getPlayerManager(), group);
        src.sendFeedback(
            new LiteralText("Removed player ")
                .formatted(Formatting.RED)
                .append(new LiteralText(player.getEntityName())
                    .formatted(Formatting.AQUA))
                .append(" from group ")
                .append(new LiteralText(group)
                    .formatted(Formatting.YELLOW))
                .append(new LiteralText(".")), true);
        return 0;
    }

    private static CompletableFuture<Suggestions> suggestGroups(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (PlayerGroup group : PingspamGroupSource.INSTANCE.getAllGroups()) {
            builder.suggest(SuggestionsUtils.wrapString(group.getName()));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerGroups(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        for (PlayerGroup group : PingspamGroupSource.INSTANCE.getGroupsOf(player.getUuid())) {
            builder.suggest(SuggestionsUtils.wrapString(group.getName()));
        }
        return builder.buildFuture();
    }

    private static int addPlayerGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        String newGroup = StringArgumentType.getString(ctx, "groupname");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        if (!GROUPNAME_PATTERN.asPredicate().test(newGroup))
            throw INVALID_GROUPNAME.create();
        if (NameLogic.isValidName(src.getMinecraftServer().getPlayerManager(), newGroup, true))
            throw NAME_COLLISION.create();
        PingspamGroup group = PingspamGroupSource.INSTANCE.getOrCreateGroup(newGroup);
        if (!group.addMember(player.getUuid()))
            throw IN_GROUP_OTHER.create(player);
        ServerNetworkLogic.addPossibleName(src.getMinecraftServer().getPlayerManager(), newGroup);
        src.sendFeedback(
            new LiteralText("Added player ")
                .formatted(Formatting.GREEN)
                .append(new LiteralText(player.getEntityName())
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
        List<PlayerGroup> pingGroups = PingspamGroupSource.INSTANCE.getGroupsOf(player.getUuid());
        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        headerBuilder.append("You are in ");
        headerBuilder.append(pingGroups.size());
        headerBuilder.append(" ping group");
        if (pingGroups.size() != 1)
            headerBuilder.append("s");
        if (pingGroups.size() > 0) {
            headerBuilder.append(": ");
            boolean isFirst = true;
            for (PlayerGroup pingGroup : pingGroups) {
                if (!isFirst)
                    contentBuilder.append(", ");
                isFirst = false;
                contentBuilder.append(pingGroup.getName());
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
