package com.xaerocustomcolors.state;

// ThreadLocal bridge between getWaypointColor() and getHex() for custom color override.
public final class ColorInterceptState {
    public static final ThreadLocal<Integer> pendingCustomHex = new ThreadLocal<>();
}
