package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.utils.MessageTypeTransformer;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.server.SaveLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SaveLoading.class)
public class SaveLoadingMixin {
    @ModifyArg(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/DataPackContents;reload(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/registry/CombinedDynamicRegistries;Ljava/util/List;Lnet/minecraft/resource/featuretoggle/FeatureSet;Lnet/minecraft/server/command/CommandManager$RegistrationEnvironment;ILjava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static CombinedDynamicRegistries<ServerDynamicRegistryType> modifyMessageTypes(CombinedDynamicRegistries<ServerDynamicRegistryType> registries) {
        MessageTypeTransformer.run(registries.getCombinedRegistryManager());

        return registries;
    }
}
