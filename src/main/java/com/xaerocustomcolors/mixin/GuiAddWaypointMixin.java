package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.gui.ColorPickerScreen;
import com.xaerocustomcolors.state.WaypointScreenState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

import java.util.ArrayList;

@Mixin(value = GuiAddWaypoint.class, remap = false)
public class GuiAddWaypointMixin {

    @Shadow private ArrayList<Waypoint> waypointsEdited;
    @Shadow private DropDownWidget colorDD;

    /**
     * Persist / remove the custom colour when the user clicks OK (edit mode).
     */
    @Inject(method = "confirmMutual", at = @At("RETURN"))
    private void xcc_applyCustomColor(CallbackInfo ci) {
        if (waypointsEdited == null || waypointsEdited.isEmpty()) return;

        // Check what's actually selected in the dropdown right now.
        boolean customIsSelected = XaeroCustomColors.isCustomSlotSelected(
                (Screen)(Object) this);

        if (customIsSelected && WaypointScreenState.hasCustomColor) {
            // User confirmed with the custom colour selected — persist it.
            for (Waypoint wp : waypointsEdited) {
                CustomColorManager.INSTANCE.setCustomColor(
                        CustomColorManager.makeKey(wp.getName(), wp.getX(), wp.getY(), wp.getZ()),
                        WaypointScreenState.customColor);
            }
            CustomColorManager.INSTANCE.save();
        } else if (!customIsSelected) {
            // User confirmed with a preset selected — remove any saved custom colour.
            boolean changed = false;
            for (Waypoint wp : waypointsEdited) {
                changed |= CustomColorManager.INSTANCE.removeCustomColor(
                        CustomColorManager.makeKey(wp.getName(), wp.getX(), wp.getY(), wp.getZ()));
            }
            if (changed) CustomColorManager.INSTANCE.save();
        }
    }

    /**
     * Intercepts colour-dropdown selections:
     * Click on "Custom Color" / "Custom: #hex" slot → open the colour picker.
     * Click on any preset → let it proceed; custom state is preserved until OK.
     */
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
            // Open the colour picker.
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
            return;
        }

        // A preset was clicked.  Don't clear the custom colour yet — only
        // clear it when the user actually clicks OK.  This lets them click
        // back on "Custom: #hex" to reopen the picker with their colour.
    }
}
