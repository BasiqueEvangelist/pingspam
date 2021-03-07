package me.basiqueevangelist.pingspam.access;

import me.basiqueevangelist.pingspam.client.network.ServerData;
import org.jetbrains.annotations.Nullable;

public interface ClientPlayNetworkHandlerAccess {
    void pingspam$requestServerData();

    @Nullable ServerData pingspam$getServerData();

    void pingspam$setServerData(ServerData data);
}
