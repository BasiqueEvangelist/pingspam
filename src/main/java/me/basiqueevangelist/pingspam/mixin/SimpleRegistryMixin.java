package me.basiqueevangelist.pingspam.mixin;

import me.basiqueevangelist.pingspam.access.ExtendedRegistry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.SimpleRegistry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Mixin(SimpleRegistry.class)
public class SimpleRegistryMixin<T> implements ExtendedRegistry {

    @Shadow private boolean frozen;

    @Shadow @Final private @Nullable Function<T, RegistryEntry.Reference<T>> valueToEntryFunction;

    @Shadow private @Nullable Map<T, RegistryEntry.Reference<T>> unfrozenValueToEntry;

    @Shadow private @Nullable List<RegistryEntry.Reference<T>> cachedEntries;

    @Override
    public void pingspam$unfreeze() {
        frozen = false;

        if (valueToEntryFunction != null)
            this.unfrozenValueToEntry = new IdentityHashMap<>();

        cachedEntries = null;
    }
}
