package me.basiqueevangelist.pingspam.mixin.client;

import me.basiqueevangelist.pingspam.access.ClientPlayNetworkHandlerAccess;
import me.basiqueevangelist.pingspam.client.network.ServerData;
import me.basiqueevangelist.pingspam.network.PingSpamPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin implements ClientPlayNetworkHandlerAccess {
    @Unique private static final long MAX_TIME_SINCE_REQUEST = 1000000000L * 10;
    @Unique private long lastPermissionsRequest = 0;
    @Unique private @Nullable ServerData serverData = null;

    @Override
    public void pingspam$requestServerData() {
        if (serverData != null && (System.nanoTime() - lastPermissionsRequest) > MAX_TIME_SINCE_REQUEST) {
            ClientPlayNetworking.send(PingSpamPackets.PULL_PERMISSIONS, PacketByteBufs.empty());
            lastPermissionsRequest = System.nanoTime();
        }
    }

    @Override
    public @Nullable ServerData pingspam$getServerData() {
        return serverData;
    }

    @Override
    public void pingspam$setServerData(ServerData data) {
        serverData = data;
    }
}
