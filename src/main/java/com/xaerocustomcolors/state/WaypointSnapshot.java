package com.xaerocustomcolors.state;

import xaero.hud.minimap.waypoint.WaypointColor;

public record WaypointSnapshot(String ctx, String key, Integer color, WaypointColor wpColor) {}
