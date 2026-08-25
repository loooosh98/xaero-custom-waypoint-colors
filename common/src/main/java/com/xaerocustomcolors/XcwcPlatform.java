package com.xaerocustomcolors;

import java.nio.file.Path;

public final class XcwcPlatform {

    private static Path gameDir;

    private XcwcPlatform() {}

    public static void init(Path dir) {
        gameDir = dir;
    }

    public static Path gameDir() {
        return gameDir;
    }
}
