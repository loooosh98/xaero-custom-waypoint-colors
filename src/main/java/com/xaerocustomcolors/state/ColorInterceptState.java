package com.xaerocustomcolors.state;

/**
 * Thread-local bridge used to pass a custom ARGB hex from
 * MinimapWaypointMixin.getWaypointColor() to WaypointColorMixin.getHex().
 *
 * The minimap renderer calls  waypoint.getWaypointColor().getHex()
 * in a single expression. We cannot override getHex() per-waypoint
 * because WaypointColor is an enum. Instead:
 *   1. getWaypointColor() @Inject sets pendingCustomHex if a custom color exists.
 *   2. getHex()            @Inject reads+clears it and cancels with our color.
 */
public final class ColorInterceptState {
    public static final ThreadLocal<Integer> pendingCustomHex = new ThreadLocal<>();
}
