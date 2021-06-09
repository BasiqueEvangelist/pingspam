package me.basiqueevangelist.pingspam.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.inkwell.conrad.impl.gui.ModConfigsScreen;
import dev.inkwell.vivian.api.ConfigScreenProvider;
import dev.inkwell.vivian.api.builders.ConfigScreenBuilder;

public class ModMenuInitializer implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> ConfigScreenProvider.getScreen("pingspam", parent);
    }
}
