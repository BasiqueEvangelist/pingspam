package me.basiqueevangelist.pingspam.mixin;

import com.mojang.authlib.GameProfile;
import me.basiqueevangelist.onedatastore.api.DataStore;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Unique private static final int ACTIONBAR_TIME = 10;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Shadow public abstract void playSound(SoundEvent event, SoundCategory category, float volume, float pitch);


    @Shadow public abstract void sendMessage(Text message, boolean actionBar);

    @Unique private PingspamPlayerData pingspamData;
    @Unique private int actionbarTime = 0;
    @Unique private int prevPingsCount = -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loadPingspamData(MinecraftServer server, ServerWorld world, GameProfile profile, CallbackInfo ci) {
        if (isImpostor()) return;

        pingspamData = DataStore.getFor(server).getPlayer(uuid, PingSpam.PLAYER_DATA);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        if (isImpostor()) return;

        var pings = pingspamData.unreadPings();

        if (prevPingsCount != pings.size()) {
            prevPingsCount = pings.size();
            actionbarTime = 0;
        }

        if (pings.size() > 0 && PingSpam.CONFIG.getConfig().showUnreadMessagesInActionbar) {
            actionbarTime--;

            if (actionbarTime <= 0) {
                actionbarTime = ACTIONBAR_TIME;
                sendMessage(
                    Text.literal("You have " + pings.size() + " unread message" + (pings.size() != 1 ? "s" : "") + "."),
                    true
                );
            }
        } else {
            actionbarTime = 0;
        }
    }

    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void onActionbarMessage(Text message, boolean actionBar, CallbackInfo ci) {
        if (actionBar)
            actionbarTime = 40;
    }

    @SuppressWarnings("ConstantConditions")
    @Unique
    private boolean isImpostor() {
        return (Class<?>) getClass() != ServerPlayerEntity.class;
    }
}
