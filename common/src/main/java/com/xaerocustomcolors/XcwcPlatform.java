package com.xaerocustomcolors;

import java.nio.file.Path;

public final class XcwcPlatform {

    private static Path gameDir;
    private static Path configDir;

    private XcwcPlatform() {}

    public static void init(Path gameDir, Path configDir) {
        XcwcPlatform.gameDir = gameDir;
        XcwcPlatform.configDir = configDir;
    }

    public static Path gameDir() {
        return gameDir;
    }

    public static Path configDir() {
        return configDir;
    }
}
