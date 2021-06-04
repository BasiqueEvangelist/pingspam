package me.basiqueevangelist.pingspam.mixin.client;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.basiqueevangelist.pingspam.client.network.PingSpamClientNetworking;
import me.basiqueevangelist.pingspam.client.network.ServerData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
@Mixin(CommandSuggestor.class)
public abstract class CommandSuggestorMixin {
    @Shadow @Final private TextFieldWidget textField;

    @Shadow
    private static int getLastPlayerNameStart(String input) {
        return 0;
    }

    @Redirect(method = "refresh", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;suggestMatching(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Suggestions> suggestWithoutCommand(Iterable<String> suggestions, SuggestionsBuilder builder) {
        PingSpamClientNetworking.requestServerData();
        ServerData data = PingSpamClientNetworking.getServerData();

        String afterString = textField.getText().substring(0, textField.getCursor());
        int lastStart = getLastPlayerNameStart(afterString);
        if (lastStart >= afterString.length() || afterString.charAt(lastStart) != '@' || data == null || !data.canPingPlayers)
            return CommandSource.suggestMatching(suggestions, builder);

        List<String> processed = new ArrayList<>(data.possibleNames);
        if (data.canPingEveryone)
            processed.add("everyone");
        if (data.canPingOnline)
            processed.add("online");
        if (data.canPingOffline)
            processed.add("offline");
        processed.sort(Comparator.naturalOrder());
        return CommandSource.suggestMatching(processed.stream().map(x -> "@" + x), builder);
    }
}
