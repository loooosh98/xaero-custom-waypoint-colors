package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.state.ColorInterceptState;
import com.xaerocustomcolors.state.WaypointScreenState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;

@Mixin(value = Waypoint.class, remap = false)
public class MinimapWaypointMixin {

    // --- Per-waypoint render cache ---
    // Avoids a String allocation (makeKey) and HashMap lookup on every
    // getWaypointColor() call, which fires once per visible waypoint per frame.
    // Invalidated when CustomColorManager's version counter changes.
    @Unique private transient String  xcc_cachedKey;
    @Unique private transient Integer xcc_cachedColor;
    @Unique private transient long    xcc_cacheVersion = -1;

    @Inject(
        method = "<init>(IIILjava/lang/String;Ljava/lang/String;Lxaero/hud/minimap/waypoint/WaypointColor;Lxaero/hud/minimap/waypoint/WaypointPurpose;ZZ)V",
        at = @At("RETURN")
    )
    private void xcc_saveColorOnCreate(
            int x, int y, int z, String name, String initials,
            WaypointColor color, WaypointPurpose purpose,
            boolean temporary, boolean yIncluded,
            CallbackInfo ci) {
        if (WaypointScreenState.hasCustomColor && name != null && !name.isEmpty()) {
            CustomColorManager.INSTANCE.setCustomColor(
                    CustomColorManager.makeKey(name, x, y, z),
                    WaypointScreenState.customColor);
            CustomColorManager.INSTANCE.save();
        }
    }

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
