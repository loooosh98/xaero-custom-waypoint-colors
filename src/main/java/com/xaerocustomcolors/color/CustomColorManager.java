package com.xaerocustomcolors.color;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages custom waypoint colors, persisting them to a JSON config file.
 * Colors are keyed by "name:x:y:z" to identify each waypoint.
 */
public class CustomColorManager {

    public static final CustomColorManager INSTANCE = new CustomColorManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final String CONFIG_FILE = "xaero-custom-colors.json";

    /** Map of "name:x:y:z" -> ARGB color int */
    private final Map<String, Integer> customColors = new HashMap<>();

    /**
     * Monotonically increasing version counter.  Bumped on every mutation
     * (set / remove / load) so that per-waypoint render caches in
     * MinimapWaypointMixin and WorldMapWaypointMixin can cheaply detect
     * staleness without recomputing the key string every frame.
     */
    private final AtomicLong version = new AtomicLong();

    private CustomColorManager() {}

    public long getVersion() { return version.get(); }

    /** Returns a consistent key for a waypoint based on its identity. */
    public static String makeKey(String name, int x, int y, int z) {
        return name + ":" + x + ":" + y + ":" + z;
    }

    /**
     * Get the custom color for a waypoint key, or null if none is set.
     * Returns a full ARGB int (e.g. 0xFF8040FF).
     */
    public Integer getCustomColor(String key) {
        return customColors.get(key);
    }

    /**
     * Set a custom ARGB color for a waypoint key.
     * The alpha channel is forced to fully opaque (0xFF).
     */
    public void setCustomColor(String key, int argbColor) {
        customColors.put(key, 0xFF000000 | argbColor);
        version.incrementAndGet();
    }

    /** Remove the custom color override for a waypoint key. Returns true if one existed. */
    public boolean removeCustomColor(String key) {
        boolean had = customColors.remove(key) != null;
        if (had) version.incrementAndGet();
        return had;
    }

    /** Load custom colors from the config file. */
    public void load() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Map<String, Integer> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) {
                customColors.clear();
                customColors.putAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("[XaeroCustomColors] Failed to load custom colors: " + e.getMessage());
        }
        version.incrementAndGet();
    }

    /** Save custom colors to the config file. */
    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(customColors, writer);
            }
        } catch (Exception e) {
            System.err.println("[XaeroCustomColors] Failed to save custom colors: " + e.getMessage());
        }
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}
