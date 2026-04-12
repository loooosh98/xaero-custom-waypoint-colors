package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.color.CustomColorManager;
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

    @Unique private transient String  xcc_cachedKey;
    @Unique private transient Integer xcc_cachedColor;
    @Unique private transient long    xcc_cacheVersion = -1;

    @Inject(method = "getWaypointColor", at = @At("RETURN"))
    private void xcc_signalCustomColor(CallbackInfoReturnable<WaypointColor> cir) {
        long currentVersion = CustomColorManager.INSTANCE.getVersion();
        if (xcc_cacheVersion != currentVersion) {
            Waypoint self = (Waypoint)(Object) this;
            xcc_cachedKey = CustomColorManager.makeKey(
                    self.getName(), self.getX(), self.getY(), self.getZ());
            xcc_cachedColor = CustomColorManager.INSTANCE.getCustomColor(xcc_cachedKey);
            xcc_cacheVersion = currentVersion;
        }
        if (xcc_cachedColor != null) {
            ColorInterceptState.pendingCustomHex.set(xcc_cachedColor & 0xFFFFFF);
        } else {
            ColorInterceptState.pendingCustomHex.remove();
        }
    }
}
