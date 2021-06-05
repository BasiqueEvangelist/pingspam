package me.basiqueevangelist.pingspam.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuInitializer implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> ConfigScreenBuilder.buildConfigScreen(parent);
    }
}
