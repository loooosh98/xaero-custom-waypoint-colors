package com.xaerocustomcolors.state;

public class WaypointScreenState {
    public static int     customColor    = 0xFFFFFFFF;
    public static boolean hasCustomColor = false;
    /** True when returning from ColorPickerScreen after the user chose a color.
     *  Prevents AFTER_INIT from overwriting the just-picked (unsaved) value. */
    public static boolean justPickedColor = false;
    /** Index into the colorDD options array that holds the custom entry. */
    public static int customSlotIndex = -1;

    public static void reset() {
        customColor     = 0xFFFFFFFF;
        hasCustomColor  = false;
        justPickedColor = false;
        customSlotIndex = -1;
    }
}
