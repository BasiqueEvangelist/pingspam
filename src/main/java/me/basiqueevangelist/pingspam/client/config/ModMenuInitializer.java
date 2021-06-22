package me.basiqueevangelist.pingspam.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.inkwell.vivian.api.ConfigScreenProvider;

public class ModMenuInitializer implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> ConfigScreenProvider.getScreen("pingspam", parent);
    }
}
