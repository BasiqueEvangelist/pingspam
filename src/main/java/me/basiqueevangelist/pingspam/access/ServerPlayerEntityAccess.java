package me.basiqueevangelist.pingspam.access;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;

import java.util.List;

public interface ServerPlayerEntityAccess {
    List<Text> pingspam$getPings();
    void pingspam$ping(GameMessageS2CPacket msgPacket);
}
