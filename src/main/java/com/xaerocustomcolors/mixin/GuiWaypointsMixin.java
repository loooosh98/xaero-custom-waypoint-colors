package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;

import java.lang.reflect.Method;
import java.util.ArrayList;

// lambda$init$5 = disable/enable button (verify on every update)
@Mixin(value = GuiWaypoints.class, remap = false)
public class GuiWaypointsMixin {

    private static Method xcc_getSelectedWaypointsList;

    @Inject(method = "lambda$init$5", at = @At("HEAD"))
    private void xcc_removeDeletedColors(ButtonWidget btn, CallbackInfo ci) {
        try {
            if (xcc_getSelectedWaypointsList == null) {
                xcc_getSelectedWaypointsList = GuiWaypoints.class.getDeclaredMethod("getSelectedWaypointsList");
                xcc_getSelectedWaypointsList.setAccessible(true);
            }
            @SuppressWarnings("unchecked")
            ArrayList<Waypoint> selected = (ArrayList<Waypoint>) xcc_getSelectedWaypointsList.invoke(this);
            if (selected == null || selected.isEmpty()) return;

            for (Waypoint wp : selected) {
                if (!wp.isTemporary()) return;
            }

            for (Waypoint wp : selected) {
                String ctx = XaeroContext.forWaypoint(wp);
                if (ctx == null) continue;
                CustomColorManager.INSTANCE.removeCustomColor(ctx, wp);
            }
        } catch (Exception e) {
            XaeroCustomColors.LOGGER.error("Failed to clean up deleted waypoint colors", e);
        }
    }
}
