package me.basiqueevangelist.pingspam.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PingspamGroupData {
    private final String name;
    final List<UUID> members = new ArrayList<>();
    private final List<UUID> membersView = Collections.unmodifiableList(members);
    private boolean pingable = true;
    private boolean hasChat = false;

    public PingspamGroupData(String name) {
        this.name = name;
    }
    public void fromTag(NbtCompound tag) {
        var members = tag.get("Members", Uuids.CODEC.listOf()).orElse(List.of());
        this.members.clear();
        this.members.addAll(members);

        pingable = tag.getBoolean("Pingable").orElse(true);
        hasChat = tag.getBoolean("HasChat").orElse(false);
    }

    public NbtCompound toTag(NbtCompound tag) {
        tag.put("Members", Uuids.CODEC.listOf(), members);
        tag.putBoolean("Pingable", pingable);
        tag.putBoolean("HasChat", hasChat);

        return tag;
    }

    public @UnmodifiableView List<UUID> members() {
        return membersView;
    }

    public boolean isPingable() {
        return pingable;
    }

    public void isPingable(boolean value) {
        pingable = value;
    }

    public boolean hasChat() {
        return hasChat;
    }

    public void hasChat(boolean value) {
        hasChat = value;
    }

    public String name() {
        return name;
    }
}
