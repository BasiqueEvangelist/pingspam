package me.basiqueevangelist.pingspam.client.config;

import blue.endless.jankson.JsonObject;
import dev.inkwell.conrad.api.Config;
import dev.inkwell.conrad.api.value.ValueKey;
import dev.inkwell.conrad.api.value.data.SaveType;
import dev.inkwell.conrad.api.value.serialization.ConfigSerializer;
import me.basiqueevangelist.pingspam.client.network.PingSpamClientNetworking;
import me.basiqueevangelist.pingspam.config.JanksonSerializer;
import org.jetbrains.annotations.NotNull;

public class PingSpamClientConfig extends Config<JsonObject> {
    public static final ValueKey<Boolean> SERVER_INTEGRATION = builder(true)
        .with((old, newValue) -> {
            if (!newValue) PingSpamClientNetworking.disable();
        })
        .build();
    public static final ValueKey<Boolean> LOCAL_MENTION_SCANNING = value(true);
    public static final ValueKey<Boolean> ALWAYS_SCAN_MENTIONS = value(false);

    @Override
    public @NotNull ConfigSerializer<JsonObject> getSerializer() {
        return new JanksonSerializer();
    }

    @Override
    public @NotNull SaveType getSaveType() {
        return SaveType.USER;
    }

    @Override
    public @NotNull String getName() {
        return "pingspam_client";
    }
}
