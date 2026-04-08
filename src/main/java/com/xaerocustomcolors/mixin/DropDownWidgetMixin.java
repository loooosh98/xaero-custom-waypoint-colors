package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.gui.ColorPickerScreen;
import com.xaerocustomcolors.state.WaypointScreenState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

/**
 * Intercepts {@code selectId} so that clicking the custom-colour slot
 * opens the colour picker even when it is already selected.
 * {@code selectId} normally skips the {@code onSelected} callback when
 * {@code id == selected}, so the GuiAddWaypointMixin inject never fires
 * for a re-click on the active custom entry.
 */
@Mixin(value = DropDownWidget.class, remap = false)
public class DropDownWidgetMixin {

    @Shadow private int selected;

    @Inject(method = "selectId", at = @At("HEAD"), cancellable = true)
    private void xcc_reopenPickerOnCustomReselect(int id, boolean callCallback, CallbackInfo ci) {
        if (!callCallback) return;
        if (id != WaypointScreenState.customSlotIndex) return;
        if (id != selected) return; // different slot — let onSelected in GuiAddWaypointMixin handle it

        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;
        if (screen == null) return;
        if (!screen.getClass().getName().equals("xaero.common.gui.GuiAddWaypoint")) return;

        int initial = WaypointScreenState.hasCustomColor
                ? WaypointScreenState.customColor : 0xFFFFFFFF;
        client.setScreen(new ColorPickerScreen(screen, initial, chosen -> {
            WaypointScreenState.customColor    = chosen;
            WaypointScreenState.hasCustomColor = true;
            WaypointScreenState.justPickedColor = true;
        }));
        ci.cancel();
    }
}
