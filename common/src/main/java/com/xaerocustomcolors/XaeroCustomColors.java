package com.xaerocustomcolors;

import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import com.xaerocustomcolors.gui.ColorPickerScreen;
import com.xaerocustomcolors.mixin.DropDownWidgetAccessor;
import com.xaerocustomcolors.mixin.GuiAddWaypointAccessor;
import com.xaerocustomcolors.state.WaypointScreenState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

public class XaeroCustomColors {

    public static final Logger LOGGER = LoggerFactory.getLogger("xaerocustomwaypointcolors");
    public static final String CUSTOM_COLOR_LABEL = ChatFormatting.GRAY + "Custom";

    private static WeakReference<Screen> lastEditScreen;

    public static void onScreenInit(Screen screen) {
        if (screen instanceof GuiAddWaypoint) {
            handleWaypointEditScreen((GuiAddWaypoint) screen);
        }
    }

    private static void handleWaypointEditScreen(GuiAddWaypoint screen) {
        boolean reInit = lastEditScreen != null && lastEditScreen.get() == screen;
        lastEditScreen = new WeakReference<>(screen);
        if (!reInit) {
            Integer pending = WaypointScreenState.pendingReceivedColor;
            WaypointScreenState.pendingReceivedColor = null;
            WaypointScreenState.customColor = null;
            try {
                ArrayList<Waypoint> wps = ((GuiAddWaypointAccessor) screen).xcwc_getWaypointsEdited();
                if (wps != null && wps.size() == 1) {
                    Waypoint wp = wps.get(0);
                    String ctx = XaeroContext.forWaypoint(wp);
                    if (ctx != null) {
                        WaypointScreenState.customColor = CustomColorManager.INSTANCE.getCustomColor(ctx, wp);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[XCWC] Failed to read waypoint data", e);
            }
            if (pending != null) {
                WaypointScreenState.customColor = ARGB.opaque(pending);
            }
            WaypointScreenState.customSelected = WaypointScreenState.customColor != null;
        }

        Integer c = WaypointScreenState.customColor;
        appendCustomDropdownEntry(screen, c != null ? formatCustomLabel(c) : CUSTOM_COLOR_LABEL,
                WaypointScreenState.customSelected);
    }

    public static void openColorPicker(Screen parent) {
        Integer c = WaypointScreenState.customColor;
        int initial = c != null ? c : 0;
        Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(parent, initial, chosen -> {
            WaypointScreenState.customColor = chosen;
            WaypointScreenState.customSelected = true;
        }));
    }

    public static String formatCustomLabel(int argb) {
        return ChatFormatting.GRAY + "Custom: #" + String.format("%06X", argb & 0xFFFFFF);
    }

    private static void appendCustomDropdownEntry(GuiAddWaypoint screen, String text, boolean select) {
        try {
            DropDownWidget colorDD = ((GuiAddWaypointAccessor) screen).xcwc_getColorDD();
            if (colorDD == null) return;
            DropDownWidgetAccessor dd = (DropDownWidgetAccessor) colorDD;

            Component label = Component.literal(text);
            Component[] realOptions = dd.xcwc_getRealOptions();
            Component[] options = dd.xcwc_getOptions();

            int customRealIndex = realOptions.length;
            Component[] newReal = Arrays.copyOf(realOptions, customRealIndex + 1);
            newReal[customRealIndex] = label;
            dd.xcwc_setRealOptions(newReal);

            Component[] newOpts = Arrays.copyOf(options, options.length + 1);
            newOpts[options.length] = label;
            dd.xcwc_setOptions(newOpts);

            WaypointScreenState.customSlotIndex = customRealIndex;

            if (select) {
                colorDD.selectId(customRealIndex, false);
            }

            LOGGER.debug("[XCWC] Custom entry added to color dropdown");
        } catch (Exception e) {
            LOGGER.error("[XCWC] Failed to append custom dropdown entry", e);
        }
    }
}
