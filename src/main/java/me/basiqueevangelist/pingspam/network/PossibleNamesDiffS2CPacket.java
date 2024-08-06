package me.basiqueevangelist.pingspam.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record PossibleNamesDiffS2CPacket(List<String> addedNames, List<String> removedNames) implements CustomPayload {
    public static final Id<PossibleNamesDiffS2CPacket> ID = new Id<>(Identifier.of("pingspam", "possible_names_diff"));
    public static final PacketCodec<PacketByteBuf, PossibleNamesDiffS2CPacket> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING.collect(PacketCodecs.toList()), PossibleNamesDiffS2CPacket::addedNames,
        PacketCodecs.STRING.collect(PacketCodecs.toList()), PossibleNamesDiffS2CPacket::removedNames,
        PossibleNamesDiffS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
