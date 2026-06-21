package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import com.xaerocustomcolors.state.WaypointScreenState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointSharingHandler;

@Mixin(value = WaypointSharingHandler.class, remap = false)
public class WaypointSharingHandlerMixin {

    @Shadow private Waypoint sharedWaypoint;

    @ModifyVariable(method = "onShareConfirmationResult", at = @At("STORE"), index = 3)
    private String xcc_appendColor(String shareStr) {
        if (shareStr == null || sharedWaypoint == null) return shareStr;
        if (!shareStr.startsWith("xaero-waypoint:") && !shareStr.startsWith("xaero_waypoint:")) return shareStr;

        String ctx = XaeroContext.forCurrentMinimap();
        Integer color = CustomColorManager.INSTANCE.getCustomColor(ctx, sharedWaypoint);
        if (color == null) return shareStr;
        return shareStr + String.format(":xcc=%06X", color & 0xFFFFFF);
    }

    @Inject(method = "onWaypointAdd", at = @At("HEAD"))
    private void xcc_captureReceivedColor(String[] parts, CallbackInfo ci) {
        if (parts == null || parts.length == 0) return;
        String last = parts[parts.length - 1];
        if (last == null || !last.startsWith("xcc=")) return;
        try {
            int color = Integer.parseInt(last.substring(4), 16);
            WaypointScreenState.pendingReceivedColor = color;
        } catch (NumberFormatException ignored) {}
    }
}
