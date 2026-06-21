package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.mods.gui.Waypoint;

@Mixin(value = Waypoint.class, remap = false)
public class WorldMapWaypointMixin {

    @Unique private transient Integer xcc_cachedColor;
    @Unique private transient long    xcc_cacheVersion = -1;

    @Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
    private void xcc_overrideColor(CallbackInfoReturnable<Integer> cir) {
        long currentVersion = CustomColorManager.INSTANCE.getVersion();
        if (xcc_cacheVersion != currentVersion) {
            Waypoint self = (Waypoint)(Object) this;
            xcc_cachedColor = null;
            if (self.getOriginal() instanceof xaero.common.minimap.waypoints.Waypoint mwp) {
                String ctx = XaeroContext.forWaypoint(mwp);
                if (ctx != null) xcc_cachedColor = CustomColorManager.INSTANCE.getCustomColor(ctx, mwp);
            }
            xcc_cacheVersion = currentVersion;
        }
        if (xcc_cachedColor != null) cir.setReturnValue(xcc_cachedColor);
    }
}
