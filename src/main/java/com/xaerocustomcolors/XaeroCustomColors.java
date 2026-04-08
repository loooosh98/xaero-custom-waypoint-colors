package com.xaerocustomcolors;

import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.state.WaypointScreenState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.Screen;
import xaero.common.minimap.waypoints.Waypoint;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class XaeroCustomColors implements ClientModInitializer {

    public static final String MOD_ID = "xaerocustomcolors";
    public static final String CUSTOM_COLOR_LABEL = "\u00a77Custom..";

    private static final String GUI_ADD_WAYPOINT = "xaero.common.gui.GuiAddWaypoint";

    // Cached reflection fields (populated once on first use)
    private static Field colorDDField;
    private static Field selectedField;
    private static Field realOptionsField;
    private static Field optionsField;
    private static Field waypointsEditedField;

    @Override
    public void onInitializeClient() {
        CustomColorManager.INSTANCE.load();

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!screen.getClass().getName().equals(GUI_ADD_WAYPOINT)) return;

            if (WaypointScreenState.justPickedColor) {
                WaypointScreenState.justPickedColor = false;
            } else {
                WaypointScreenState.reset();
                try {
                    if (waypointsEditedField == null) {
                        waypointsEditedField = screen.getClass().getDeclaredField("waypointsEdited");
                        waypointsEditedField.setAccessible(true);
                    }
                    @SuppressWarnings("unchecked")
                    ArrayList<Waypoint> wps = (ArrayList<Waypoint>) waypointsEditedField.get(screen);
                    if (wps != null && !wps.isEmpty()) {
                        Waypoint wp = wps.get(0);
                        String key = CustomColorManager.makeKey(
                                wp.getName(), wp.getX(), wp.getY(), wp.getZ());
                        Integer saved = CustomColorManager.INSTANCE.getCustomColor(key);
                        if (saved != null) {
                            WaypointScreenState.customColor    = saved;
                            WaypointScreenState.hasCustomColor = true;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[XaeroCustomColors] Failed to read waypoint data: " + e.getMessage());
                }
            }

            // Always append a custom entry at the bottom of the dropdown.
            // If a custom color is active, show "Custom: #RRGGBB" and select it.
            // Otherwise, show "Custom.." (unselected).
            String entryText = WaypointScreenState.hasCustomColor
                    ? formatCustomLabel(WaypointScreenState.customColor)
                    : CUSTOM_COLOR_LABEL;
            appendCustomDropdownEntry(screen, entryText, WaypointScreenState.hasCustomColor);
        });
    }

    /** Formats the dropdown label for an active custom color. */
    public static String formatCustomLabel(int argb) {
        return "\u00a77Custom: #" + String.format("%06X", argb & 0xFFFFFF);
    }

    /**
     * Appends an entry at the end of the color dropdown's arrays.
     * If {@code select} is true, points {@code selected} at the new entry.
     */
    private static void appendCustomDropdownEntry(Screen screen, String text, boolean select) {
        try {
            Object colorDD = getColorDD(screen);
            if (colorDD == null) return;

            ensureDropdownFields(colorDD);

            String[] realOptions = (String[]) realOptionsField.get(colorDD);
            String[] options = (String[]) optionsField.get(colorDD);

            // Grow each array from its OWN length.  The options array may be
            // longer than realOptions when the dropdown has an empty-option "-"
            // entry at index 0 (hasEmptyOption = true).  Using realOptions.length
            // for both would overwrite the last preset instead of appending.
            int customRealIndex = realOptions.length;
            String[] newReal = Arrays.copyOf(realOptions, customRealIndex + 1);
            newReal[customRealIndex] = text;
            realOptionsField.set(colorDD, newReal);

            int customOptIndex = options.length;
            String[] newOpts = Arrays.copyOf(options, customOptIndex + 1);
            newOpts[customOptIndex] = text;
            optionsField.set(colorDD, newOpts);

            // onSelected receives the realOptions index (excludes the empty
            // option), so customSlotIndex must use that index space.
            WaypointScreenState.customSlotIndex = customRealIndex;

            if (select) {
                // selected is a realOptions index — drawMenu uses it to
                // access realOptions directly.
                selectedField.setInt(colorDD, customRealIndex);
            }
        } catch (Exception e) {
            System.err.println("[XaeroCustomColors] Failed to append custom dropdown entry: " + e.getMessage());
        }
    }

    /** Returns true if the colour dropdown currently has the custom slot selected. */
    public static boolean isCustomSlotSelected(Screen screen) {
        try {
            Object colorDD = getColorDD(screen);
            if (colorDD == null) return false;
            ensureDropdownFields(colorDD);
            int sel = selectedField.getInt(colorDD);
            return sel == WaypointScreenState.customSlotIndex;
        } catch (Exception e) {
            return false;
        }
    }

    /** Updates the text of the custom dropdown entry in-place. */
    public static void updateCustomEntryText(Screen screen, String text) {
        try {
            Object colorDD = getColorDD(screen);
            if (colorDD == null) return;
            ensureDropdownFields(colorDD);
            String[] realOptions = (String[]) realOptionsField.get(colorDD);
            String[] options = (String[]) optionsField.get(colorDD);
            // The custom entry is always the last element in both arrays,
            // but their indices differ when hasEmptyOption is true.
            if (realOptions.length > 0) realOptions[realOptions.length - 1] = text;
            if (options.length > 0) options[options.length - 1] = text;
        } catch (Exception e) {
            System.err.println("[XaeroCustomColors] Failed to update custom entry text: " + e.getMessage());
        }
    }

    private static Object getColorDD(Screen screen) throws Exception {
        if (colorDDField == null) {
            colorDDField = screen.getClass().getDeclaredField("colorDD");
            colorDDField.setAccessible(true);
        }
        return colorDDField.get(screen);
    }

    private static void ensureDropdownFields(Object colorDD) throws Exception {
        Class<?> ddClass = colorDD.getClass();
        if (selectedField == null) {
            selectedField = findFieldInHierarchy(ddClass, "selected");
        }
        if (realOptionsField == null) {
            realOptionsField = findFieldInHierarchy(ddClass, "realOptions");
        }
        if (optionsField == null) {
            optionsField = findFieldInHierarchy(ddClass, "options");
        }
    }

    /**
     * Walks up the class hierarchy to find a declared field by name.
     * Fixes the case where fields like {@code selected} are declared in a
     * superclass of the runtime type — {@code getDeclaredField} alone would miss them.
     */
    private static Field findFieldInHierarchy(Class<?> clazz, String name) throws NoSuchFieldException {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name + " not found in hierarchy of " + clazz.getName());
    }
}
