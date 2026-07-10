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
            MinimapWorldManager mgr = currentWorldManager();
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
                return s.isEmpty() ? null : s;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static String forWaypoint(Waypoint wp) {
        if (wp == null) return null;
        try {
            MinimapWorldManager mgr = currentWorldManager();
            if (mgr == null) return null;
            MinimapWorld current = mgr.getCurrentWorld();
            if (current != null && containsWaypoint(current, wp)) return forMinimapWorld(current);
            MinimapWorldRootContainer root = mgr.getCurrentRootContainer();
            if (root == null) return null;
            for (MinimapWorld w : root.getAllWorldsIterable()) {
                if (w != current && containsWaypoint(w, wp)) return forMinimapWorld(w);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static MinimapWorldManager currentWorldManager() {
        MinimapSession s = BuiltInHudModules.MINIMAP.getCurrentSession();
        return s == null ? null : s.getWorldManager();
    }

    private static boolean containsWaypoint(MinimapWorld w, Waypoint wp) {
        for (WaypointSet set : w.getIterableWaypointSets()) {
            for (Waypoint candidate : set.getWaypoints()) {
                if (candidate == wp) return true;
            }
        }
        return false;
    }
}
