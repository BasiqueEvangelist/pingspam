package me.basiqueevangelist.pechkin.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.UUID;

public record MailMessage(Text contents, UUID sender, UUID messageId, Instant sentAt) {
    public static MailMessage fromTag(NbtCompound tag) {
        Text contents = Text.Serializer.fromJson(tag.getString("Contents"));
        UUID sender = tag.getUuid("Sender");
        UUID messageId = tag.getUuid("UUID");
        Instant sentAt = Instant.ofEpochMilli(tag.getLong("SentAt"));

        return new MailMessage(contents, sender, messageId, sentAt);
    }

    public NbtCompound toTag(NbtCompound tag) {
        tag.putString("Contents", Text.Serializer.toJson(contents));
        tag.put("Sender", NbtHelper.fromUuid(sender));
        tag.put("UUID", NbtHelper.fromUuid(messageId));
        tag.putLong("SentAt", sentAt.toEpochMilli());
        return tag;
    }
}
