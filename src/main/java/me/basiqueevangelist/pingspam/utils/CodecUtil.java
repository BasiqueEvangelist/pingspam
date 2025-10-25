package me.basiqueevangelist.pingspam.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public final class CodecUtil {
    private CodecUtil() {

    }

    public static final Codec<Text> TEXT_JSON = Codec.STRING
        .xmap(JsonParser::parseString, JsonElement::toString)
        .flatXmap(x -> TextCodecs.CODEC.parse(JsonOps.INSTANCE, x), x -> TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, x));
}
