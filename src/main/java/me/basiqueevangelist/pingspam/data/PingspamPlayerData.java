package me.basiqueevangelist.pingspam.data;

import com.mojang.serialization.Codec;
import me.basiqueevangelist.onedatastore.api.ComponentInstance;
import me.basiqueevangelist.pingspam.utils.CaseInsensitiveUtil;
import me.basiqueevangelist.pingspam.utils.CodecUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PingspamPlayerData implements ComponentInstance {
    private final List<Text> unreadPings;
    private final Set<String> aliases;
    private final List<UUID> ignoredPlayers;
    private @Nullable SoundEvent pingSound;
    private final Set<String> groups;
    private @Nullable String currentChat;

    public PingspamPlayerData(
        List<Text> unreadPings,
        Set<String> aliases,
        List<UUID> ignoredPlayers,
        @Nullable SoundEvent pingSound,
        Set<String> groups
    ) {
        this.unreadPings = unreadPings;
        this.aliases = aliases;
        this.ignoredPlayers = ignoredPlayers;
        this.pingSound = pingSound;
        this.groups = groups;
    }

    public PingspamPlayerData() {
        this(new ArrayList<>(), CaseInsensitiveUtil.setIgnoringCase(), new ArrayList<>(), SoundEvents.BLOCK_BELL_USE, CaseInsensitiveUtil.setIgnoringCase());
    }

    @Override
    public void fromTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        if (tag.contains("UnreadPings")) {
            List<Text> pings = tag.get("UnreadPings", CodecUtil.TEXT_JSON.listOf()).orElse(List.of());
            unreadPings.addAll(pings);
        }

        if (tag.contains("Aliases")) {
            NbtList aliasesTag = tag.getListOrEmpty("Aliases");
            for (NbtElement aliasTag : aliasesTag) {
                aliasTag.asString().ifPresent(aliases::add);
            }
        }

        if (tag.contains("IgnoredPlayers")) {
            List<UUID> ignoredPlayerList = tag.get("IgnoredPlayers", Uuids.CODEC.listOf()).orElse(List.of());
            ignoredPlayers.addAll(ignoredPlayerList);
        }

        if (tag.contains("PingSound")) {
            var soundText = tag.getString("PingSound").orElse("null");
            if (soundText.equals("null")) {
                pingSound = null;
            } else {
                pingSound = Registries.SOUND_EVENT.getOptionalValue(Identifier.of(soundText)).orElse(SoundEvents.BLOCK_BELL_USE);
            }
        }

        if (tag.contains("CurrentChat")) {
            currentChat = tag.getString("CurrentChat").orElse(null);
        }
    }

    @Override
    public NbtCompound toTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        if (!unreadPings.isEmpty()) {
            tag.put("UnreadPings", CodecUtil.TEXT_JSON.listOf(), unreadPings);
        }

        if (!aliases.isEmpty()) {
            tag.put("Aliases", Codec.STRING.listOf().xmap(LinkedHashSet::new, ArrayList::new), aliases);
        }

        if (!ignoredPlayers.isEmpty()) {
            tag.put("IgnoredPlayers", Uuids.CODEC.listOf(), ignoredPlayers);
        }

        if (pingSound == null) {
            tag.putString("PingSound", "null");
        } else if (pingSound != SoundEvents.BLOCK_BELL_USE) {
            tag.putString("PingSound", pingSound.id().toString());
        }

        if (currentChat != null) {
            tag.putString("CurrentChat", currentChat);
        }

        return tag;
    }

    public void addPing(Text pingMsg) {
        while (unreadPings.size() >= 100)
            unreadPings.remove(0);
        unreadPings.add(pingMsg);
    }

    public List<Text> unreadPings() {
        return unreadPings;
    }

    public Set<String> aliases() {
        return aliases;
    }

    public List<UUID> ignoredPlayers() {
        return ignoredPlayers;
    }

    public @Nullable SoundEvent pingSound() {
        return pingSound;
    }

    public Set<String> groups() {
        return groups;
    }

    public void setPingSound(@Nullable SoundEvent pingSound) {
        this.pingSound = pingSound;
    }

    public @Nullable String currentChat() {
        return currentChat;
    }

    public void currentChat(@Nullable String currentChat) {
        this.currentChat = currentChat;
    }
}
