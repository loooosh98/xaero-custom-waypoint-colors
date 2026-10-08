package com.xaerocustomcolors.mixin;

import com.xaerocustomcolors.XaeroCustomColors;
import com.xaerocustomcolors.color.CustomColorManager;
import com.xaerocustomcolors.color.XaeroContext;
import com.xaerocustomcolors.state.ColorInterceptState;
import com.xaerocustomcolors.state.WaypointScreenState;
import com.xaerocustomcolors.state.WaypointSnapshot;
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
import xaero.common.gui.GuiWaypointWorlds;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

import java.util.ArrayList;

@Mixin(value = GuiAddWaypoint.class, remap = false)
public class GuiAddWaypointMixin {

    @Shadow private ArrayList<Waypoint> waypointsEdited;
    @Shadow private DropDownWidget colorDD;
    @Shadow private MinimapWorldManager manager;
    @Shadow private GuiWaypointWorlds worlds;
    @Shadow private MinimapWorld defaultWorld;

    // Pre-save keys for detecting renames / coord changes.
    @Unique private WaypointSnapshot[] xcc_prev;

    // OK button handler, this is where Xaero makes new waypoints or updates existing ones
    // lambda$init$0 is auto generated so double check the name on every Xaero update
    @Inject(method = "lambda$init$0", at = @At("HEAD"))
    private void xcc_enterSave(ClientConfigManager config, Button btn, CallbackInfo ci) {
        xcc_prev = null;
        if (waypointsEdited == null) return;
        String srcCtx = XaeroContext.forMinimapWorld(defaultWorld);
        int n = waypointsEdited.size();
        xcc_prev = new WaypointSnapshot[n];
        for (int i = 0; i < n; i++) {
            Waypoint wp = waypointsEdited.get(i);
            String ctx = srcCtx != null ? srcCtx : XaeroContext.forWaypoint(wp);
            xcc_prev[i] = new WaypointSnapshot(ctx, CustomColorManager.wpKey(wp),
                    ctx == null ? null : CustomColorManager.INSTANCE.getCustomColor(ctx, wp),
                    wp.getWaypointColor());
        }
        ColorInterceptState.pendingCustomHex.remove();
    }

    @Inject(method = "lambda$init$0", at = @At("RETURN"))
    private void xcc_applyCustomColor(ClientConfigManager config, Button btn, CallbackInfo ci) {
        try {
            if (waypointsEdited == null || waypointsEdited.isEmpty()) return;

            boolean customIsSelected = colorDD != null
                    && colorDD.getSelected() == WaypointScreenState.customSlotIndex;
            Integer chosen = WaypointScreenState.customColor;

            int saved = 0;
            int removed = 0;

            String destCtx = null;
            try {
                if (manager != null && worlds != null) {
                    destCtx = XaeroContext.forMinimapWorld(manager.getWorld(worlds.getCurrentKey()));
                }
            } catch (Throwable ignored) {}

            int n = waypointsEdited.size();
            for (int i = 0; i < n; i++) {
                Waypoint wp = waypointsEdited.get(i);
                WaypointSnapshot prev = (xcc_prev != null && i < xcc_prev.length) ? xcc_prev[i] : null;
                if (wp.isThirdParty()) continue;
                String ctx = destCtx != null ? destCtx : XaeroContext.forWaypoint(wp);
                if (ctx == null && prev != null) ctx = prev.ctx();
                if (ctx == null) continue;
                String newKey = CustomColorManager.wpKey(wp);

                String oldKey = prev != null ? prev.key() : null;
                String oldCtx = prev != null && prev.ctx() != null ? prev.ctx() : ctx;
                boolean relocated = oldKey != null && (!oldKey.equals(newKey) || !oldCtx.equals(ctx));

                if (customIsSelected && chosen != null) {
                    if (relocated) CustomColorManager.INSTANCE.removeByKey(oldCtx, oldKey);
                    if (CustomColorManager.INSTANCE.setCustomColor(ctx, wp, chosen)) saved++;
                } else if (chosen != null) {
                    if (relocated) CustomColorManager.INSTANCE.removeByKey(oldCtx, oldKey);
                    if (CustomColorManager.INSTANCE.removeCustomColor(ctx, wp)) removed++;
                } else {
                    Integer prevColor = prev != null ? prev.color() : null;
                    if (prevColor == null) {
                        if (CustomColorManager.INSTANCE.removeCustomColor(ctx, wp)) removed++;
                    } else if (wp.getWaypointColor() != prev.wpColor()) {
                        if (relocated) CustomColorManager.INSTANCE.removeByKey(oldCtx, oldKey);
                        if (CustomColorManager.INSTANCE.removeCustomColor(ctx, wp)) removed++;
                    } else if (relocated) {
                        CustomColorManager.INSTANCE.removeByKey(oldCtx, oldKey);
                        if (CustomColorManager.INSTANCE.setCustomColor(ctx, wp, prevColor)) saved++;
                    }
                }
            }

            if (saved > 0) XaeroCustomColors.LOGGER.info("[XCWC] Custom waypoint color saved successfully ({})", saved);
            if (removed > 0) XaeroCustomColors.LOGGER.info("[XCWC] Custom waypoint color deleted ({})", removed);

        } finally {
            ColorInterceptState.pendingCustomHex.remove();
            WaypointScreenState.customColor = null;
            WaypointScreenState.customSelected = false;
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
        } else {
            WaypointScreenState.customSelected = false;
        }
    }
}
