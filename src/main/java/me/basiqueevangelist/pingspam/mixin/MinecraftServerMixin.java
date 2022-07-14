package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.utils.MessageTypeTransformer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow @Final protected SaveProperties saveProperties;

    @Shadow public abstract DynamicRegistryManager.Immutable getRegistryManager();

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void finalizeWorldGen(CallbackInfo ci) {
        MessageTypeTransformer.run(getRegistryManager());
    }
}
