package com.xaerocustomcolors;

import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import com.xaerocustomcolors.gui.ColorPickerScreen;
import com.xaerocustomcolors.mixin.DropDownWidgetAccessor;
import com.xaerocustomcolors.mixin.GuiAddWaypointAccessor;
import com.xaerocustomcolors.state.WaypointScreenState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
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

public class XaeroCustomColors implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("xaerocustomwaypointcolors");
    public static final String CUSTOM_COLOR_LABEL = ChatFormatting.GRAY + "Custom";

    private static WeakReference<Screen> lastEditScreen;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GuiAddWaypoint) {
                handleWaypointEditScreen((GuiAddWaypoint) screen);
            }
        });
    }

    private void handleWaypointEditScreen(GuiAddWaypoint screen) {
        boolean reInit = lastEditScreen != null && lastEditScreen.get() == screen;
        lastEditScreen = new WeakReference<>(screen);
        if (!reInit) {
            Integer pending = WaypointScreenState.pendingReceivedColor;
            WaypointScreenState.pendingReceivedColor = null;
            WaypointScreenState.customColor = null;
            try {
                ArrayList<Waypoint> wps = ((GuiAddWaypointAccessor) screen).xcc_getWaypointsEdited();
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
        }

        Integer c = WaypointScreenState.customColor;
        appendCustomDropdownEntry(screen, c != null ? formatCustomLabel(c) : CUSTOM_COLOR_LABEL, c != null);
    }

    public static void openColorPicker(Screen parent) {
        Integer c = WaypointScreenState.customColor;
        int initial = c != null ? c : 0;
        Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(parent, initial, chosen -> {
            WaypointScreenState.customColor = chosen;
        }));
    }

    public static String formatCustomLabel(int argb) {
        return ChatFormatting.GRAY + "Custom: #" + String.format("%06X", argb & 0xFFFFFF);
    }

    private static void appendCustomDropdownEntry(GuiAddWaypoint screen, String text, boolean select) {
        try {
            DropDownWidget colorDD = ((GuiAddWaypointAccessor) screen).xcc_getColorDD();
            if (colorDD == null) return;
            DropDownWidgetAccessor dd = (DropDownWidgetAccessor) colorDD;

            Component label = Component.literal(text);
            Component[] realOptions = dd.xcc_getRealOptions();
            Component[] options = dd.xcc_getOptions();

            int customRealIndex = realOptions.length;
            Component[] newReal = Arrays.copyOf(realOptions, customRealIndex + 1);
            newReal[customRealIndex] = label;
            dd.xcc_setRealOptions(newReal);

            Component[] newOpts = Arrays.copyOf(options, options.length + 1);
            newOpts[options.length] = label;
            dd.xcc_setOptions(newOpts);

            WaypointScreenState.customSlotIndex = customRealIndex;

            if (select) {
                colorDD.selectId(customRealIndex, false);
            }

            LOGGER.info("[XCWC] Custom entry added to color dropdown");
        } catch (Exception e) {
            LOGGER.error("[XCWC] Failed to append custom dropdown entry", e);
        }
    }
}
