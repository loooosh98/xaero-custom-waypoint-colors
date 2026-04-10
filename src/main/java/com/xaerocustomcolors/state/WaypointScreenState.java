package com.xaerocustomcolors.state;

public class WaypointScreenState {
    public static int     customColor    = 0xFFFFFFFF;
    public static boolean hasCustomColor = false;
    /** True when returning from ColorPickerScreen after the user chose a color.
     *  Prevents AFTER_INIT from overwriting the just-picked (unsaved) value. */
    public static boolean justPickedColor = false;
    /** Index into the colorDD options array that holds the custom entry. */
    public static int customSlotIndex = -1;
    /** True only while we're inside GuiAddWaypoint.confirmMutual (i.e. the user
     *  clicked OK).  Guards the Waypoint constructor inject so it only writes
     *  colors for waypoints created by user confirmation — NOT for waypoints
     *  Xaero is loading from disk during a world switch. */
    public static boolean insideConfirm = false;

    public static void reset() {
        customColor     = 0xFFFFFFFF;
        hasCustomColor  = false;
        justPickedColor = false;
        customSlotIndex = -1;
    }
}
