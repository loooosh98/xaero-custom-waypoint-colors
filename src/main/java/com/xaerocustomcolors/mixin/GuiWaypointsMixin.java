package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.color.CustomColorManager;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;

import java.util.ArrayList;

// lambda$init$5 = disable/enable button (verify on every update)
@Mixin(value = GuiWaypoints.class, remap = false)
public class GuiWaypointsMixin {

    @Inject(method = "lambda$init$5", at = @At("HEAD"))
    private void xcc_removeDeletedColors(ButtonWidget btn, CallbackInfo ci) {
        try {
            java.lang.reflect.Method m = GuiWaypoints.class.getDeclaredMethod("getSelectedWaypointsList");
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            ArrayList<Waypoint> selected = (ArrayList<Waypoint>) m.invoke(this);
            if (selected == null || selected.isEmpty()) return;

            for (Waypoint wp : selected) {
                if (!wp.isTemporary()) return;
            }

            boolean changed = false;
            for (Waypoint wp : selected) {
                String key = CustomColorManager.makeKey(
                        wp.getName(), wp.getX(), wp.getY(), wp.getZ());
                changed |= CustomColorManager.INSTANCE.removeCustomColor(key);
            }
            if (changed) CustomColorManager.INSTANCE.save();
        } catch (Exception e) {
            System.err.println("[XaeroCustomColors] Failed to clean up deleted waypoint colors: " + e.getMessage());
        }
    }
}
