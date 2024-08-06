package me.basiqueevangelist.onedatastore.impl.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.onedatastore.api.Component;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.onedatastore.impl.OneDataStoreInit;
import me.basiqueevangelist.onedatastore.impl.OneDataStoreState;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PurgeCommand {
    private static final SimpleCommandExceptionType NO_PLAYER_FOUND = new SimpleCommandExceptionType(Text.literal("Specified player not found."));
    private static final SimpleCommandExceptionType UNKNOWN_COMPONENT = new SimpleCommandExceptionType(Text.literal("Unknown component"));
    private static final SimpleCommandExceptionType TOO_MANY_PLAYERS = new SimpleCommandExceptionType(Text.literal("Can't mention many players at once!"));


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("onedatastore")
                .then(literal("purge")
                    .requires(Permissions.require("onedatastore.purge", 4))
                    .then(literal("player")
                        .then(argument("target", GameProfileArgumentType.gameProfile())
                            .executes(PurgeCommand::purgePlayer)))
                    .then(argument("global", IdentifierArgumentType.identifier())
                        .suggests(PurgeCommand::suggestGlobalComponents)
                        .executes(PurgeCommand::purgeGlobalComponent))));
    }

    public static GameProfile getOnePlayer(CommandContext<ServerCommandSource> ctx, String argName) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(ctx, argName);

        if (profiles.size() > 1)
            throw TOO_MANY_PLAYERS.create();

        return profiles.iterator().next();
    }

    private static int purgeGlobalComponent(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var src = ctx.getSource();
        var store = OneDataStoreState.getFrom(ctx.getSource().getServer());
        var componentId = IdentifierArgumentType.getIdentifier(ctx, "global");

        Component<?, DataStore> component = OneDataStoreInit.GLOBAL_COMPONENTS.get(componentId);

        if (component == null)
            throw UNKNOWN_COMPONENT.create();

        store.reinitComponent(component);

        src.sendFeedback(() -> Text.literal("Purged and reinitialized ")
            .formatted(Formatting.GREEN)
            .append(Text.literal(componentId.toString())
                .formatted(Formatting.YELLOW))
            .append("."), false);

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestGlobalComponents(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        List<String> ids = new ArrayList<>();

        for (Identifier id : OneDataStoreInit.GLOBAL_COMPONENTS.keySet()) {
            ids.add(id.toString());
        }

        return CommandSource.suggestMatching(ids, builder);
    }

    private static int purgePlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var src = ctx.getSource();
        var profile = getOnePlayer(ctx, "target");
        var store = OneDataStoreState.getFrom(ctx.getSource().getServer());

        if (store.playersMap().remove(profile.getId()) == null)
            throw NO_PLAYER_FOUND.create();

        src.sendFeedback(() -> Text.literal("Purged ")
            .formatted(Formatting.GREEN)
            .append(Text.literal(profile.getName())
                .formatted(Formatting.AQUA))
            .append("'s custom data."), false);

        return 1;
    }
}
