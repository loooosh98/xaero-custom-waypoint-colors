package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.state.ColorInterceptState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.waypoint.WaypointColor;

/**
 * Intercepts WaypointColor.getHex() to return a custom RGB when
 * MinimapWaypointMixin has set ColorInterceptState.pendingCustomHex.
 *
 * The two mixins together form a ThreadLocal bridge that piggybacks on the
 *   waypoint.getWaypointColor().getHex()
 * call chain used by every Xaero renderer (minimap HUD, waypoint list GUI).
 */
@Mixin(value = WaypointColor.class, remap = false)
public class WaypointColorMixin {

    @Inject(method = "getHex", at = @At("HEAD"), cancellable = true)
    private void xcc_overrideHex(CallbackInfoReturnable<Integer> cir) {
        Integer custom = ColorInterceptState.pendingCustomHex.get();
        if (custom != null) {
            ColorInterceptState.pendingCustomHex.remove();
            cir.setReturnValue(custom);
        }
    }
}
