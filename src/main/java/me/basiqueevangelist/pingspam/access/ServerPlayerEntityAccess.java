package me.basiqueevangelist.pingspam.access;

import net.minecraft.text.Text;

import java.util.List;

public interface ServerPlayerEntityAccess {
    List<Text> pingspam$getPings();
    void pingspam$ping(Text message);
}
