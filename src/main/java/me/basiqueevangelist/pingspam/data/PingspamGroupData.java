package me.basiqueevangelist.pingspam.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PingspamGroupData {
    private final String name;
    private final List<UUID> members = new ArrayList<>();
    private boolean pingable = true;
    private boolean hasChat = false;

    public PingspamGroupData(String name) {
        this.name = name;
    }
    public void fromTag(NbtCompound tag) {
        var membersTag = tag.getList("Members", NbtElement.INT_ARRAY_TYPE);
        members.clear();

        for (NbtElement playerTag : membersTag) {
            members.add(NbtHelper.toUuid(playerTag));
        }

        pingable = tag.getBoolean("Pingable");
        hasChat = tag.getBoolean("HasChat");
    }

    public NbtCompound toTag(NbtCompound tag) {
        var membersTag = new NbtList();
        tag.put("Members", membersTag);

        for (UUID playerId : members) {
            membersTag.add(NbtHelper.fromUuid(playerId));
        }

        tag.putBoolean("Pingable", pingable);
        tag.putBoolean("HasChat", hasChat);

        return tag;
    }

    public List<UUID> members() {
        return members;
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
