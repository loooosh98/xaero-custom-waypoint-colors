package com.xaerocustomcolors;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.xaerocustomcolors.gui.XcwcSettingsScreen;

public class XcwcModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return XcwcSettingsScreen::new;
    }
}
