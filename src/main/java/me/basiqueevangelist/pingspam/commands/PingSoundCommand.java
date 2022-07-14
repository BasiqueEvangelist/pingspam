package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PingSoundCommand {
    private static final SimpleCommandExceptionType INVALID_SOUND = new SimpleCommandExceptionType(Text.literal("Invalid sound type!"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingspam")
                .then(literal("sound")
                    .then(argument("sound", IdentifierArgumentType.identifier())
                        .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                        .executes(PingSoundCommand::setPingSound))
                    .then(literal("none")
                        .executes(PingSoundCommand::removePingSound))
                    .executes(PingSoundCommand::getPingSound))
        );
    }

    private static int setPingSound(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);
        Identifier soundId = IdentifierArgumentType.getIdentifier(ctx, "sound");
        SoundEvent event = Registry.SOUND_EVENT.getOrEmpty(soundId).orElseThrow(INVALID_SOUND::create);

        data.setPingSound(event);

        src.sendFeedback(Text.literal("Set ping sound to ")
            .formatted(Formatting.GREEN)
            .append(Text.literal(soundId.toString())
                .formatted(Formatting.YELLOW))
            .append(Text.literal(".")), false);

        return 0;
    }

    private static int removePingSound(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        data.setPingSound(null);

        src.sendFeedback(Text.literal("Disabled ping sound.")
            .formatted(Formatting.GREEN), false);

        return 0;
    }

    private static int getPingSound(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        PingspamPlayerData data = DataStore.getFor(src.getServer()).getPlayer(player.getUuid(), PingSpam.PLAYER_DATA);

        if (data.pingSound() != null) {
            src.sendFeedback(Text.literal("Your current ping sound is ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(data.pingSound().getId().toString())
                    .formatted(Formatting.YELLOW))
                .append(Text.literal(".")), false);
        } else {
            src.sendFeedback(Text.literal("You have disabled ping sounds.")
                .formatted(Formatting.GREEN), false);
        }

        return 0;
    }
}
