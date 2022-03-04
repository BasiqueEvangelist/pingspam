package me.basiqueevangelist.pingspam.mixin;

import com.mojang.authlib.GameProfile;
import me.basiqueevangelist.pingspam.PingSpam;
import me.basiqueevangelist.pingspam.data.PingspamPersistentState;
import me.basiqueevangelist.pingspam.data.PingspamPlayerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
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

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Shadow public abstract void playSound(SoundEvent event, SoundCategory category, float volume, float pitch);

    @Shadow public ServerPlayNetworkHandler networkHandler;
    @Unique private PingspamPlayerData pingspamData;
    @Unique private int actionbarTime = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void loadPingspamData(MinecraftServer server, ServerWorld world, GameProfile profile, CallbackInfo ci) {
         pingspamData = PingspamPersistentState.getFrom(server).getFor(uuid);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        var pings = pingspamData.unreadPings();

        if (pings.size() > 0 && PingSpam.CONFIG.getConfig().showUnreadMessagesInActionbar) {
            actionbarTime--;

            if (actionbarTime <= 0) {
                actionbarTime = ACTIONBAR_TIME;
                networkHandler.sendPacket(new GameMessageS2CPacket(
                    new LiteralText("You have " + pings.size() + " unread message" + (pings.size() != 1 ? "s" : "") + "."),
                    MessageType.GAME_INFO,
                    Util.NIL_UUID
                ));
            }
        } else {
            actionbarTime = 0;
        }
    }

    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void onActionbarMessage(Text message, boolean actionBar, CallbackInfo ci) {
        if (actionBar)
            actionbarTime = 70;
    }
}
