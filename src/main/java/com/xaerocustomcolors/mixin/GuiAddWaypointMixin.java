package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import com.xaerocustomcolors.gui.ColorPickerScreen;
import com.xaerocustomcolors.state.ColorInterceptState;
import com.xaerocustomcolors.state.WaypointScreenState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

import java.util.ArrayList;

@Mixin(value = GuiAddWaypoint.class, remap = false)
public class GuiAddWaypointMixin {

    @Shadow private ArrayList<Waypoint> waypointsEdited;
    @Shadow private DropDownWidget colorDD;

    // Pre-save keys for detecting renames / coord changes.
    @Unique private String[] xcc_oldKeys;
    @Unique private Integer[] xcc_oldColors;
    @Unique private WaypointColor[] xcc_oldWpColors;

    // OK button handler, this is where Xaero makes new waypoints or updates existing ones
    // lambda$init$0 is auto generated so double check the name on every Xaero update
    @Inject(method = "lambda$init$0", at = @At("HEAD"))
    private void xcc_enterSave(ClientConfigManager config, Button btn, CallbackInfo ci) {
        if (waypointsEdited != null) {
            int n = waypointsEdited.size();
            xcc_oldKeys = new String[n];
            xcc_oldColors = new Integer[n];
            xcc_oldWpColors = new WaypointColor[n];
            for (int i = 0; i < n; i++) {
                Waypoint wp = waypointsEdited.get(i);
                xcc_oldKeys[i] = CustomColorManager.wpKey(wp);
                xcc_oldWpColors[i] = wp.getWaypointColor();
                String ctx = XaeroContext.forWaypoint(wp);
                xcc_oldColors[i] = ctx == null ? null
                        : CustomColorManager.INSTANCE.getCustomColor(ctx, wp);
            }
            ColorInterceptState.pendingCustomHex.remove();
        } else {
            xcc_oldKeys = null;
            xcc_oldColors = null;
            xcc_oldWpColors = null;
        }
    }

    @Inject(method = "lambda$init$0", at = @At("RETURN"))
    private void xcc_applyCustomColor(ClientConfigManager config, Button btn, CallbackInfo ci) {
        try {
            if (waypointsEdited == null || waypointsEdited.isEmpty()) return;

            boolean customIsSelected = XaeroCustomColors.isCustomSlotSelected(
                    (Screen)(Object) this);

            int n = waypointsEdited.size();
            for (int i = 0; i < n; i++) {
                Waypoint wp = waypointsEdited.get(i);
                String ctx = XaeroContext.forWaypoint(wp);
                if (ctx == null) continue;
                String newKey = CustomColorManager.wpKey(wp);

                String oldKey = (xcc_oldKeys != null && i < xcc_oldKeys.length)
                        ? xcc_oldKeys[i] : null;
                boolean rekeyed = oldKey != null && !oldKey.equals(newKey);

                if (customIsSelected && WaypointScreenState.hasCustomColor) {
                    if (rekeyed) CustomColorManager.INSTANCE.removeByKey(ctx, oldKey);
                    CustomColorManager.INSTANCE.setCustomColor(ctx, wp, WaypointScreenState.customColor);
                } else if (!customIsSelected && WaypointScreenState.hasCustomColor) {
                    if (rekeyed) CustomColorManager.INSTANCE.removeByKey(ctx, oldKey);
                    CustomColorManager.INSTANCE.removeCustomColor(ctx, wp);
                } else {
                    Integer prev = (xcc_oldColors != null && i < xcc_oldColors.length)
                            ? xcc_oldColors[i] : null;
                    if (prev == null) {
                        CustomColorManager.INSTANCE.removeCustomColor(ctx, wp);
                    } else if (wp.getWaypointColor() != xcc_oldWpColors[i]) {
                        if (rekeyed) CustomColorManager.INSTANCE.removeByKey(ctx, oldKey);
                        CustomColorManager.INSTANCE.removeCustomColor(ctx, wp);
                    } else if (rekeyed) {
                        CustomColorManager.INSTANCE.removeByKey(ctx, oldKey);
                        CustomColorManager.INSTANCE.setCustomColor(ctx, wp, prev);
                    }
                }
            }
            ColorInterceptState.pendingCustomHex.remove();
        } finally {
            WaypointScreenState.hasCustomColor = false;
            WaypointScreenState.customColor    = 0xFFFFFFFF;
            xcc_oldKeys = null;
            xcc_oldColors = null;
            xcc_oldWpColors = null;
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
            int initial = WaypointScreenState.hasCustomColor
                    ? WaypointScreenState.customColor : 0;
            Screen self = (Screen)(Object) this;
            Minecraft.getInstance().gui.setScreen(
                    new ColorPickerScreen(self, initial, chosen -> {
                        WaypointScreenState.customColor    = chosen;
                        WaypointScreenState.hasCustomColor = true;
                        WaypointScreenState.justPickedColor = true;
                    }));
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
