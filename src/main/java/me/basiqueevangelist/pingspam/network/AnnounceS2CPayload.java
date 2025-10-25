package me.basiqueevangelist.pingspam.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public record AnnounceS2CPayload(boolean canPingEveryone, boolean canPingOnline, boolean canPingOffline,
                                 boolean canPingPlayers, Set<String> possibleNames) implements CustomPayload {
    public static final Id<AnnounceS2CPayload> ID = new Id<>(Identifier.of("pingspam", "announce"));

    public static final PacketCodec<PacketByteBuf, AnnounceS2CPayload> V0 = PacketCodec.tuple(
        PacketCodecs.BOOLEAN, AnnounceS2CPayload::canPingEveryone,
        PacketCodecs.BOOLEAN, AnnounceS2CPayload::canPingOnline,
        PacketCodecs.BOOLEAN, AnnounceS2CPayload::canPingOffline,
        PacketCodecs.BOOLEAN, AnnounceS2CPayload::canPingPlayers,
        PacketCodecs.STRING.collect(PacketCodecs.toList()).xmap(HashSet::new, ArrayList::new), AnnounceS2CPayload::possibleNames,
        AnnounceS2CPayload::new
    );

    public static final PacketCodec<PacketByteBuf, AnnounceS2CPayload> CODEC = new PacketVersionSwitch<PacketByteBuf, AnnounceS2CPayload>()
        .version(0, V0)
        .build();

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
