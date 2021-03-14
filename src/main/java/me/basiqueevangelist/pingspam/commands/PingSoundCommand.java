package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.basiqueevangelist.pingspam.PlayerUtils;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PingSoundCommand {
    private static final SimpleCommandExceptionType INVALID_SOUND = new SimpleCommandExceptionType(new LiteralText("Invalid sound type!"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("pingsound")
                .then(argument("sound", IdentifierArgumentType.identifier())
                    .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                    .executes(PingSoundCommand::setPingSound))
                .executes(PingSoundCommand::getPingSound)
        );
    }

    private static int setPingSound(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        Identifier soundId = IdentifierArgumentType.getIdentifier(ctx, "sound");
        SoundEvent event = Registry.SOUND_EVENT.getOrEmpty(soundId).orElseThrow(INVALID_SOUND::create);

        PlayerUtils.setPingSound(player, event);
        src.sendFeedback(new LiteralText("Set ping sound to " + PlayerUtils.getPingSound(player).getId() + "."), false);

        return 0;
    }

    private static int getPingSound(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();

        src.sendFeedback(new LiteralText("Your current ping sound is " + PlayerUtils.getPingSound(player).getId() + "."), false);

        return 0;
    }
}
