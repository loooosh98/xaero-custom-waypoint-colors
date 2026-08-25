package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;

import java.util.ArrayList;

// lambda$init$4 = disable/enable button (verify on every update)
@Mixin(value = GuiWaypoints.class, remap = false)
public class GuiWaypointsMixin {

    @Shadow private ArrayList<Waypoint> getSelectedWaypointsList() { throw new AssertionError(); }

    @Inject(method = "lambda$init$4", at = @At("HEAD"))
    private void xcwc_removeDeletedColors(Button btn, CallbackInfo ci) {
        try {
            ArrayList<Waypoint> selected = getSelectedWaypointsList();
            if (selected == null || selected.isEmpty()) return;

            for (Waypoint wp : selected) {
                if (!wp.isTemporary()) return;
            }

            int removed = 0;
            for (Waypoint wp : selected) {
                String ctx = XaeroContext.forWaypoint(wp);
                if (ctx == null) continue;
                if (CustomColorManager.INSTANCE.removeCustomColor(ctx, wp)) removed++;
            }

            if (removed > 0) XaeroCustomColors.LOGGER.info("[XCWC] Custom waypoint color deleted ({})", removed);
        } catch (Exception e) {
            XaeroCustomColors.LOGGER.error("[XCWC] Failed to clean up deleted waypoint colors", e);
        }
    }
}
