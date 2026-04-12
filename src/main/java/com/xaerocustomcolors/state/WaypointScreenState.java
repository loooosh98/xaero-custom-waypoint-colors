package com.xaerocustomcolors.state;

public class WaypointScreenState {
    public static int     customColor    = 0xFFFFFFFF;
    public static boolean hasCustomColor = false;
    public static boolean justPickedColor = false;
    public static int customSlotIndex = -1;

    public static void reset() {
        customColor     = 0xFFFFFFFF;
        hasCustomColor  = false;
        justPickedColor = false;
        customSlotIndex = -1;
    }
}
