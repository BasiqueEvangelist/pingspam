package me.basiqueevangelist.pingspam.utils;

import me.basiqueevangelist.pingspam.access.ExtendedRegistry;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Decoration;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class MessageTypeTransformer {
    public static void run(DynamicRegistryManager drm) {
        Registry<MessageType> reg = drm.get(Registry.MESSAGE_TYPE_KEY);

        Map<Identifier, MessageType> addedTypes = new HashMap<>();

        reg.streamEntries().forEach(entry -> {
            Identifier id = entry.registryKey().getValue();

            if (id.getNamespace().equals("pingspam")) return;

            Identifier pingedId = wrapPinged(id);
            Identifier pingSuccessfulId = wrapPingSuccessful(id);

            if (!reg.containsId(pingedId)) {
                MessageType newType = tryTransformChat(entry.value(), s -> s.withColor(Formatting.AQUA));

                if (newType != null) {
                    addedTypes.put(pingedId, newType);
                }
            }

            if (!reg.containsId(pingSuccessfulId)) {
                MessageType newType = tryTransformChat(entry.value(), s -> s.withColor(Formatting.GOLD));

                if (newType != null) {
                    addedTypes.put(pingSuccessfulId, newType);
                }
            }
        });

        ((ExtendedRegistry) reg).pingspam$unfreeze();

        for (var entry : addedTypes.entrySet()) {
            Registry.register(reg, entry.getKey(), entry.getValue());
        }

        reg.freeze();
    }

    private static MessageType tryTransformChat(MessageType type, Function<Style, Style> styleTransformer) {
        MessageType.DisplayRule chatRule = type.chat().orElse(null);

        if (chatRule == null) return null;

        Decoration chatDecoration = chatRule.decoration().orElse(null);

        if (chatDecoration == null) return null;

        Decoration newDecoration = new Decoration(chatDecoration.translationKey(), chatDecoration.parameters(), styleTransformer.apply(chatDecoration.style()));
        MessageType.DisplayRule newRule = new MessageType.DisplayRule(Optional.of(newDecoration));

        return new MessageType(Optional.of(newRule), type.overlay(), type.narration());
    }

    public static Identifier wrapPinged(Identifier id) {
        return new Identifier("pingspam", "pinged/" + id.getNamespace() + "/" + id.getPath());
    }

    public static Identifier wrapPingSuccessful(Identifier id) {
        return new Identifier("pingspam", "ping_successful/" + id.getNamespace() + "/" + id.getPath());
    }
}
