package me.basiqueevangelist.pingspam.client.config;

import blue.endless.jankson.JsonObject;
import dev.inkwell.conrad.api.Config;
import dev.inkwell.conrad.api.value.ValueKey;
import dev.inkwell.conrad.api.value.data.DataType;
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
        .with(DataType.COMMENT, "Enable/disable integration with the Pingspam server mod.")
        .build();
    public static final ValueKey<Boolean> LOCAL_MENTION_SCANNING = builder(true)
        .with(DataType.COMMENT, "Scan messages for mentions on the client")
        .build();
    public static final ValueKey<Boolean> ALWAYS_SCAN_MENTIONS = builder(false)
        .with(DataType.COMMENT, "Scan messages for mentions even if the server has Pingspam installed")
        .build();

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
