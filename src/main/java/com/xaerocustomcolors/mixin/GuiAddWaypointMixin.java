package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import com.xaerocustomcolors.state.ColorInterceptState;
import com.xaerocustomcolors.state.WaypointScreenState;
import com.xaerocustomcolors.state.WaypointSnapshot;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

import java.util.ArrayList;

@Mixin(value = GuiAddWaypoint.class, remap = false)
public class GuiAddWaypointMixin {

    @Shadow private ArrayList<Waypoint> waypointsEdited;
    @Shadow private DropDownWidget colorDD;

    // Pre-save keys for detecting renames / coord changes.
    @Unique private WaypointSnapshot[] xcc_prev;

    // OK button handler, this is where Xaero makes new waypoints or updates existing ones
    // lambda$init$5 is auto generated so double check the name on every Xaero update
    @Inject(method = "lambda$init$5", at = @At("HEAD"))
    private void xcc_enterSave(ClientConfigManager config, ButtonWidget btn, CallbackInfo ci) {
        xcc_prev = null;
        if (waypointsEdited == null) return;
        int n = waypointsEdited.size();
        xcc_prev = new WaypointSnapshot[n];
        for (int i = 0; i < n; i++) {
            Waypoint wp = waypointsEdited.get(i);
            String ctx = XaeroContext.forWaypoint(wp);
            xcc_prev[i] = new WaypointSnapshot(ctx, CustomColorManager.wpKey(wp),
                    ctx == null ? null : CustomColorManager.INSTANCE.getCustomColor(ctx, wp),
                    wp.getWaypointColor());
        }
        ColorInterceptState.pendingCustomHex.remove();
    }

    @Inject(method = "lambda$init$5", at = @At("RETURN"))
    private void xcc_applyCustomColor(ClientConfigManager config, ButtonWidget btn, CallbackInfo ci) {
        try {
            if (waypointsEdited == null || waypointsEdited.isEmpty()) return;

            boolean customIsSelected = colorDD != null
                    && colorDD.getSelected() == WaypointScreenState.customSlotIndex;
            Integer chosen = WaypointScreenState.customColor;

            int n = waypointsEdited.size();
            for (int i = 0; i < n; i++) {
                Waypoint wp = waypointsEdited.get(i);
                WaypointSnapshot prev = (xcc_prev != null && i < xcc_prev.length) ? xcc_prev[i] : null;
                String ctx = prev != null && prev.ctx() != null ? prev.ctx() : XaeroContext.forWaypoint(wp);
                if (ctx == null) continue;
                String newKey = CustomColorManager.wpKey(wp);

                String oldKey = prev != null ? prev.key() : null;
                boolean rekeyed = oldKey != null && !oldKey.equals(newKey);

                if (customIsSelected && chosen != null) {
                    if (rekeyed) CustomColorManager.INSTANCE.removeByKey(ctx, oldKey);
                    CustomColorManager.INSTANCE.setCustomColor(ctx, wp, chosen);
                } else if (!customIsSelected && chosen != null) {
                    if (rekeyed) CustomColorManager.INSTANCE.removeByKey(ctx, oldKey);
                    CustomColorManager.INSTANCE.removeCustomColor(ctx, wp);
                } else {
                    Integer prevColor = prev != null ? prev.color() : null;
                    if (prevColor == null) {
                        CustomColorManager.INSTANCE.removeCustomColor(ctx, wp);
                    } else if (wp.getWaypointColor() != prev.wpColor()) {
                        if (rekeyed) CustomColorManager.INSTANCE.removeByKey(ctx, oldKey);
                        CustomColorManager.INSTANCE.removeCustomColor(ctx, wp);
                    } else if (rekeyed) {
                        CustomColorManager.INSTANCE.removeByKey(ctx, oldKey);
                        CustomColorManager.INSTANCE.setCustomColor(ctx, wp, prevColor);
                    }
                }
            }
            ColorInterceptState.pendingCustomHex.remove();
        } finally {
            WaypointScreenState.customColor = null;
            xcc_prev = null;
        }
    }

    @Inject(
        method = "onSelected(Lxaero/lib/client/gui/widget/dropdown/DropDownWidget;I)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void xcc_handleColorDD(
            DropDownWidget dd, int index,
            CallbackInfoReturnable<Boolean> cir) {
        if (dd != colorDD) return;

        if (index == WaypointScreenState.customSlotIndex) {
            XaeroCustomColors.openColorPicker((Screen)(Object) this);
            cir.setReturnValue(false);
        }
    }
}
