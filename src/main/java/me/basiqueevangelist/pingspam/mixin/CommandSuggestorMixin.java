package me.basiqueevangelist.pingspam.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CommandSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestor.class)
public abstract class CommandSuggestorMixin {
    @Shadow @Final private TextFieldWidget textField;

    @Shadow
    protected static int getLastPlayerNameStart(String input) {
        return 0;
    }

    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "refresh", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;suggestMatching(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Suggestions> suggestWithoutCommand(Iterable<String> suggestions, SuggestionsBuilder builder) {
        String afterString = textField.getText().substring(0, textField.getCursor());
        int lastStart = getLastPlayerNameStart(afterString);
        if (lastStart < afterString.length() && afterString.charAt(lastStart) == '@') {
            List<String> processed = new ArrayList<>();
            for (String suggestion : suggestions) {
                processed.add("@" + suggestion);
            }
            if (client.player.hasPermissionLevel(2)) {
                processed.add("@everyone");
            }

            return CommandSource.suggestMatching(processed, builder);
        } else {
            return CommandSource.suggestMatching(suggestions, builder);
        }
    }
}
