package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import com.xaerocustomcolors.state.ColorInterceptState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;

@Mixin(value = Waypoint.class, remap = false)
public class MinimapWaypointMixin {

    @Unique private transient Integer xcwc_cachedColor;
    @Unique private transient long    xcwc_cacheVersion = -1;

    @Inject(method = "getWaypointColor", at = @At("RETURN"))
    private void xcwc_signalCustomColor(CallbackInfoReturnable<WaypointColor> cir) {
        long currentVersion = CustomColorManager.INSTANCE.getVersion();
        if (xcwc_cacheVersion != currentVersion) {
            Waypoint self = (Waypoint)(Object) this;
            String ctx = XaeroContext.forWaypoint(self);
            Integer c = ctx == null ? null : CustomColorManager.INSTANCE.getCustomColor(ctx, self);
            xcwc_cachedColor = c == null ? null : c & 0xFFFFFF;
            xcwc_cacheVersion = currentVersion;
        }
        if (xcwc_cachedColor != null) {
            ColorInterceptState.pendingCustomHex.set(xcwc_cachedColor);
        } else {
            ColorInterceptState.pendingCustomHex.remove();
        }
    }
}
