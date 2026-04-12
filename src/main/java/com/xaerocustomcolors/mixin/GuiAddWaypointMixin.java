package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.gui.ColorPickerScreen;
import com.xaerocustomcolors.state.WaypointScreenState;
import net.minecraft.client.MinecraftClient;
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

    // Pre-save keys (name:x:y:z) for detecting renames / coord changes.
    @Unique private String[] xcc_oldKeys;

    // OK button handler — where Xaero creates new / mutates existing waypoints.
    // lambda$init$3 is compiler-generated; verify name on every Xaero update.
    @Inject(method = "lambda$init$3", at = @At("HEAD"))
    private void xcc_enterSave(ClientConfigManager config, ButtonWidget btn, CallbackInfo ci) {
        if (waypointsEdited != null) {
            xcc_oldKeys = new String[waypointsEdited.size()];
            for (int i = 0; i < waypointsEdited.size(); i++) {
                Waypoint wp = waypointsEdited.get(i);
                xcc_oldKeys[i] = CustomColorManager.makeKey(
                        wp.getName(), wp.getX(), wp.getY(), wp.getZ());
            }
        } else {
            xcc_oldKeys = null;
        }
    }

    @Inject(method = "lambda$init$3", at = @At("RETURN"))
    private void xcc_applyCustomColor(ClientConfigManager config, ButtonWidget btn, CallbackInfo ci) {
        try {
            if (waypointsEdited == null || waypointsEdited.isEmpty()) return;

            boolean customIsSelected = XaeroCustomColors.isCustomSlotSelected(
                    (Screen)(Object) this);
            boolean changed = false;

            for (int i = 0; i < waypointsEdited.size(); i++) {
                Waypoint wp = waypointsEdited.get(i);
                String newKey = CustomColorManager.makeKey(
                        wp.getName(), wp.getX(), wp.getY(), wp.getZ());

                if (xcc_oldKeys != null && i < xcc_oldKeys.length) {
                    String oldKey = xcc_oldKeys[i];
                    if (oldKey != null && !oldKey.equals(newKey)) {
                        changed |= CustomColorManager.INSTANCE.removeCustomColor(oldKey);
                    }
                }

                if (customIsSelected && WaypointScreenState.hasCustomColor) {
                    CustomColorManager.INSTANCE.setCustomColor(
                            newKey, WaypointScreenState.customColor);
                    changed = true;
                } else if (!customIsSelected) {
                    changed |= CustomColorManager.INSTANCE.removeCustomColor(newKey);
                }
            }

            if (changed) CustomColorManager.INSTANCE.save();
        } finally {
            WaypointScreenState.hasCustomColor = false;
            WaypointScreenState.customColor    = 0xFFFFFFFF;
            xcc_oldKeys = null;
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
                    ? WaypointScreenState.customColor : 0xFFFFFFFF;
            Screen self = (Screen)(Object) this;
            MinecraftClient.getInstance().setScreen(
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
