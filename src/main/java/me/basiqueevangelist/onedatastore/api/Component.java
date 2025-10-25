package me.basiqueevangelist.onedatastore.api;

import me.basiqueevangelist.onedatastore.impl.OneDataStoreInit;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.NonExtendable
public interface Component<T extends ComponentInstance, C>  {
    Identifier id();

    Function<C, T> factory();

    static <T extends ComponentInstance> Component<T, PlayerDataEntry> registerPlayer(Identifier id, Function<PlayerDataEntry, T> factory) {
        return OneDataStoreInit.registerPlayerComponent(id, factory);
    }

    static <T extends ComponentInstance> Component<T, DataStore> registerGlobal(Identifier id, Function<DataStore, T> factory) {
        return OneDataStoreInit.registerGlobalComponent(id, factory);
    }
}
