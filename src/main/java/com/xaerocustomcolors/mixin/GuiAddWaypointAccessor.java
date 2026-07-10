package com.xaerocustomcolors.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

import java.util.ArrayList;

@Mixin(value = GuiAddWaypoint.class, remap = false)
public interface GuiAddWaypointAccessor {

    @Accessor("colorDD")
    DropDownWidget xcc_getColorDD();

    @Accessor("waypointsEdited")
    ArrayList<Waypoint> xcc_getWaypointsEdited();
}
