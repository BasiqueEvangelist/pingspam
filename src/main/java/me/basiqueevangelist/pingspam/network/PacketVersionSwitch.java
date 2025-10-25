package me.basiqueevangelist.pingspam.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.stream.Collectors;

public final class PacketVersionSwitch<B extends PacketByteBuf, V> {
    private final Int2ObjectMap<PacketCodec<B, V>> versions = new Int2ObjectOpenHashMap<>();
    private int latestVersion = -1;

    public PacketVersionSwitch<B, V> version(int version, PacketCodec<B, V> alternative) {
        versions.put(version, alternative);

        if (latestVersion < version) latestVersion = version;

        return this;
    }

    public PacketCodec<B, V> build() {
        var supported = versions.keySet().intStream().mapToObj(Integer::toString).collect(Collectors.joining(", "));

        return new PacketCodec<>() {
            @Override
            public V decode(B buf) {
                int version = buf.readVarInt();

                var codec = versions.get(version);
                if (codec == null) {
                    throw new IllegalStateException("Received packet of version " + version + ", but only versions " + supported + " are supported!");
                }

                return codec.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                buf.writeVarInt(latestVersion);

                versions.get(latestVersion).encode(buf, value);
            }
        };
    }
}
