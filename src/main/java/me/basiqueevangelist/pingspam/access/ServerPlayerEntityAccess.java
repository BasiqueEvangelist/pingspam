package me.basiqueevangelist.pingspam.access;

import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public interface ServerPlayerEntityAccess {
    List<Text> pingspam$getPings();

    List<String> pingspam$getShortnames();

    SoundEvent pingspam$getPingSound();
    void pingspam$setPingSound(SoundEvent sound);

    List<UUID> pingspam$getIgnoredPlayers();
    void pingspam$addIgnoredPlayer(UUID uuid);
    void pingspam$removeIgnoredPlayer(UUID uuid);
}
