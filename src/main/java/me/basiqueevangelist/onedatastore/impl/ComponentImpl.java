package me.basiqueevangelist.onedatastore.impl;

import me.basiqueevangelist.onedatastore.api.Component;
import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public record ComponentImpl<T extends ComponentInstance, C> (
    Identifier id,
    Function<C, T> factory
) implements Component<T, C> {
    @Override
    public String toString() {
        return "ComponentImpl{" +
            "id=" + id +
            '}';
    }
}
