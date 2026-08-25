package com.xaerocustomcolors.fabric;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.XcwcPlatform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;

public class XcwcFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        XcwcPlatform.init(FabricLoader.getInstance().getGameDir());
        ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> XaeroCustomColors.onScreenInit(screen));
    }
}
