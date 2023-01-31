package me.basiqueevangelist.pingspam.mixin;

import com.mojang.serialization.Lifecycle;
import me.basiqueevangelist.pingspam.access.ExtendedRegistry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(SimpleRegistry.class)
public class SimpleRegistryMixin<T> implements ExtendedRegistry {
    private boolean pingspam$intrusive;

    @Shadow private @Nullable List<RegistryEntry.Reference<T>> cachedEntries;

    @Shadow @Nullable private Map<T, RegistryEntry.Reference<T>> intrusiveValueToEntry;

    @Shadow private boolean frozen;

    @Inject(method = "<init>(Lnet/minecraft/registry/RegistryKey;Lcom/mojang/serialization/Lifecycle;Z)V", at = @At("TAIL"))
    private void saveIntrusiveness(RegistryKey<?> key, Lifecycle lifecycle, boolean intrusive, CallbackInfo ci) {
        pingspam$intrusive = intrusive;
    }

    @Override
    public void pingspam$unfreeze() {
        frozen = false;

        if (pingspam$intrusive)
            this.intrusiveValueToEntry = new IdentityHashMap<>();

        cachedEntries = null;
    }
}
