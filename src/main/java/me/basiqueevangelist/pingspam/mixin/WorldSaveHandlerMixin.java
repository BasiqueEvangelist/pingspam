package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.OfflinePlayerCache;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(WorldSaveHandler.class)
public class WorldSaveHandlerMixin {
    @Inject(method = "savePlayerData", at = @At(value = "INVOKE", target = "Ljava/io/File;createTempFile(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)Ljava/io/File;"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onPlayerDataSaved(PlayerEntity player, CallbackInfo ci, CompoundTag tag) {
        OfflinePlayerCache.INSTANCE.onPlayerDataSaved(player.getUuid(), tag);
    }
}
