package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.utils.MessageTypeTransformer;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow @Final private CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void finalizeWorldGen(CallbackInfo ci) {
        MessageTypeTransformer.run(combinedDynamicRegistries.getCombinedRegistryManager());
    }
}
