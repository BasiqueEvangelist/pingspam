package me.basiqueevangelist.pingspam.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.basiqueevangelist.pingspam.utils.CodecUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;

import java.time.Instant;
import java.util.UUID;

public record MailMessage(Text contents, UUID sender, UUID messageId, Instant sentAt) {
    public static final Codec<MailMessage> CODEC = RecordCodecBuilder.create(i -> i.group(
        CodecUtil.TEXT_JSON.fieldOf("Contents").forGetter(MailMessage::contents),
        Uuids.CODEC.fieldOf("Sender").forGetter(MailMessage::sender),
        Uuids.CODEC.fieldOf("UUID").forGetter(MailMessage::messageId),
        Codec.LONG.xmap(Instant::ofEpochMilli, Instant::toEpochMilli).fieldOf("SentAt").forGetter(MailMessage::sentAt)
    ).apply(i, MailMessage::new));
}
