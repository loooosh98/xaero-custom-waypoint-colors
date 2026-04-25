package com.xaerocustomcolors.color;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.hud.minimap.world.container.MinimapWorldContainer;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

public final class XaeroContext {

    private XaeroContext() {}

    public static String forCurrentMinimap() {
        try {
            MinimapSession s = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (s == null) return null;
            MinimapWorldManager mgr = s.getWorldManager();
            if (mgr == null) return null;
            return forMinimapWorld(mgr.getCurrentWorld());
        } catch (Throwable t) {
            return null;
        }
    }

    public static String forMinimapWorld(MinimapWorld w) {
        if (w == null) return null;
        try {
            MinimapWorldContainer c = w.getContainer();
            if (c != null && c.getPath() != null) {
                String s = c.getPath().toString();
                return (s == null || s.isEmpty()) ? null : s;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static String forWaypoint(Waypoint wp) {
        if (wp == null) return null;
        try {
            MinimapSession s = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (s == null) return null;
            MinimapWorldManager mgr = s.getWorldManager();
            if (mgr == null) return null;
            MinimapWorldRootContainer root = mgr.getCurrentRootContainer();
            if (root == null) return null;
            for (MinimapWorld w : root.getAllWorldsIterable()) {
                for (WaypointSet set : w.getIterableWaypointSets()) {
                    for (Waypoint candidate : set.getWaypoints()) {
                        if (candidate == wp) return forMinimapWorld(w);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
