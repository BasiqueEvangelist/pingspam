package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.access.ServerPlayerEntityAccess;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Shadow @Nullable public abstract ServerPlayerEntity getPlayer(String name);

    @Shadow @Nullable public abstract ServerPlayerEntity getPlayer(UUID uuid);

    @Shadow @Final private List<ServerPlayerEntity> players;
    @Unique private static final Pattern PING_PATTERN = Pattern.compile("@([a-zA-Z0-9]{3,16}(\\s|$))");

    @Inject(method = "broadcastChatMessage", at = @At("HEAD"))
    public void onMessageBroadcasted(Text message, MessageType type, UUID senderUuid, CallbackInfo ci) {
        String contents = message.getString();
        Matcher matcher = PING_PATTERN.matcher(contents);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (username.equals("everyone")) {
                ServerPlayerEntity sender = getPlayer(senderUuid);
                if (sender != null && !sender.hasPermissionLevel(2))
                    continue;
                for (ServerPlayerEntity player : players) {
                    ((ServerPlayerEntityAccess)player).pingspam$ping(message);
                }
            } else {
                ServerPlayerEntity player = getPlayer(username);
                if (player != null)
                    ((ServerPlayerEntityAccess)player).pingspam$ping(message);
            }
        }
    }
}
