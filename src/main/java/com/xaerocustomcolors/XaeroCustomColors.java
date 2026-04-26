package com.xaerocustomcolors;

import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import com.xaerocustomcolors.state.WaypointScreenState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.minimap.waypoints.Waypoint;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class XaeroCustomColors implements ClientModInitializer {

    public static final String MOD_ID = "xaerocustomcolors";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String CUSTOM_COLOR_LABEL = "\u00a77Custom";

    private static final String GUI_ADD_WAYPOINT = "xaero.common.gui.GuiAddWaypoint";

    private static Field colorDDField;
    private static Field selectedField;
    private static Field realOptionsField;
    private static Field optionsField;
    private static Field waypointsEditedField;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen.getClass().getName().equals(GUI_ADD_WAYPOINT)) {
                handleWaypointEditScreen(screen);
            }
        });
    }

    private void handleWaypointEditScreen(Screen screen) {
        if (WaypointScreenState.justPickedColor) {
            WaypointScreenState.justPickedColor = false;
        } else {
            Integer pending = WaypointScreenState.pendingReceivedColor;
            WaypointScreenState.pendingReceivedColor = null;
            WaypointScreenState.hasCustomColor = false;
            WaypointScreenState.customColor    = 0xFFFFFFFF;
            try {
                if (waypointsEditedField == null) {
                    waypointsEditedField = screen.getClass().getDeclaredField("waypointsEdited");
                    waypointsEditedField.setAccessible(true);
                }
                @SuppressWarnings("unchecked")
                ArrayList<Waypoint> wps = (ArrayList<Waypoint>) waypointsEditedField.get(screen);
                if (wps != null && !wps.isEmpty()) {
                    Waypoint wp = wps.get(0);
                    String ctx = XaeroContext.forWaypoint(wp);
                    if (ctx != null) {
                        Integer saved = CustomColorManager.INSTANCE.getCustomColor(ctx, wp);
                        if (saved != null) {
                            WaypointScreenState.customColor    = saved;
                            WaypointScreenState.hasCustomColor = true;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read waypoint data", e);
            }
            if (pending != null) {
                WaypointScreenState.customColor    = 0xFF000000 | (pending & 0xFFFFFF);
                WaypointScreenState.hasCustomColor = true;
            }
        }

        String entryText = WaypointScreenState.hasCustomColor
                ? formatCustomLabel(WaypointScreenState.customColor)
                : CUSTOM_COLOR_LABEL;
        appendCustomDropdownEntry(screen, entryText, WaypointScreenState.hasCustomColor);
    }

    public static String formatCustomLabel(int argb) {
        return "\u00a77Custom: #" + String.format("%06X", argb & 0xFFFFFF);
    }

    private static void appendCustomDropdownEntry(Screen screen, String text, boolean select) {
        try {
            Object colorDD = getColorDD(screen);
            if (colorDD == null) return;

            ensureDropdownFields(colorDD);

            String[] realOptions = (String[]) realOptionsField.get(colorDD);
            String[] options = (String[]) optionsField.get(colorDD);

            int customRealIndex = realOptions.length;
            String[] newReal = Arrays.copyOf(realOptions, customRealIndex + 1);
            newReal[customRealIndex] = text;
            realOptionsField.set(colorDD, newReal);

            int customOptIndex = options.length;
            String[] newOpts = Arrays.copyOf(options, customOptIndex + 1);
            newOpts[customOptIndex] = text;
            optionsField.set(colorDD, newOpts);

            WaypointScreenState.customSlotIndex = customRealIndex;

            if (select) {
                selectedField.setInt(colorDD, customRealIndex);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to append custom dropdown entry", e);
        }
    }

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
