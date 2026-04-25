package com.xaerocustomcolors.color;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import xaero.common.minimap.waypoints.Waypoint;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class CustomColorManager {

    public static final CustomColorManager INSTANCE = new CustomColorManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final String ROOT_DIR = "xaero_custom_waypoint_colors";
    private static final String COLOR_FILE = "colors.json";
    private static final String LEGACY_FILE = "xaero-custom-colors.json";

    private final ConcurrentMap<String, ConcurrentMap<String, Integer>> bucketsByCtx = new ConcurrentHashMap<>();
    private final Map<String, Integer> legacyEntries = new ConcurrentHashMap<>();
    private volatile boolean legacyLoaded = false;
    private final AtomicLong version = new AtomicLong();

    private CustomColorManager() {}

    public long getVersion() { return version.get(); }

    public static String wpKey(Waypoint wp) {
        return wp.getName() + ":" + wp.getX() + ":" + wp.getY() + ":" + wp.getZ();
    }

    public Integer getCustomColor(String ctxPath, Waypoint wp) {
        if (ctxPath == null || wp == null) return null;
        String key = wpKey(wp);
        Map<String, Integer> bucket = loadBucket(ctxPath);
        Integer c = bucket.get(key);
        if (c != null) return c;
        return migrateFromLegacy(ctxPath, key);
    }

    public void setCustomColor(String ctxPath, Waypoint wp, int argbColor) {
        if (ctxPath == null || wp == null) return;
        Map<String, Integer> bucket = loadBucket(ctxPath);
        bucket.put(wpKey(wp), 0xFF000000 | argbColor);
        version.incrementAndGet();
        saveBucket(ctxPath);
    }

    public boolean removeCustomColor(String ctxPath, Waypoint wp) {
        if (ctxPath == null || wp == null) return false;
        Map<String, Integer> bucket = loadBucket(ctxPath);
        boolean had = bucket.remove(wpKey(wp)) != null;
        if (had) {
            version.incrementAndGet();
            saveBucket(ctxPath);
        }
        return had;
    }

    public boolean removeByKey(String ctxPath, String wpKey) {
        if (ctxPath == null || wpKey == null) return false;
        Map<String, Integer> bucket = loadBucket(ctxPath);
        boolean had = bucket.remove(wpKey) != null;
        if (had) {
            version.incrementAndGet();
            saveBucket(ctxPath);
        }
        return had;
    }

    private Integer migrateFromLegacy(String ctxPath, String wpKey) {
        loadLegacyOnce();
        Integer c = legacyEntries.get(wpKey);
        if (c == null) return null;
        Map<String, Integer> bucket = loadBucket(ctxPath);
        bucket.put(wpKey, c);
        version.incrementAndGet();
        saveBucket(ctxPath);
        return c;
    }

    private synchronized void loadLegacyOnce() {
        if (legacyLoaded) return;
        legacyLoaded = true;
        Path p = legacyFile();
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p)) {
            Map<String, Integer> all = GSON.fromJson(r, MAP_TYPE);
            if (all == null) return;
            for (Map.Entry<String, Integer> e : all.entrySet()) {
                String k = e.getKey();
                Integer v = e.getValue();
                if (k == null || v == null) continue;
                String[] parts = k.split(":");
                if (parts.length != 4) continue;
                if (!isInt(parts[1]) || !isInt(parts[2]) || !isInt(parts[3])) continue;
                legacyEntries.put(k, v);
            }
        } catch (Exception ex) {
            com.xaerocustomcolors.XaeroCustomColors.LOGGER.error("Failed to load legacy colors", ex);
        }
    }

    private static boolean isInt(String s) {
        try { Integer.parseInt(s); return true; } catch (NumberFormatException e) { return false; }
    }

    private Map<String, Integer> loadBucket(String ctxPath) {
        return bucketsByCtx.computeIfAbsent(ctxPath, p -> {
            ConcurrentMap<String, Integer> map = new ConcurrentHashMap<>();
            Path file = bucketFile(p);
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    Map<String, Integer> loaded = GSON.fromJson(r, MAP_TYPE);
                    if (loaded != null) {
                        for (Map.Entry<String, Integer> e : loaded.entrySet()) {
                            if (e.getKey() != null && e.getValue() != null) map.put(e.getKey(), e.getValue());
                        }
                    }
                } catch (Exception e) {
                    com.xaerocustomcolors.XaeroCustomColors.LOGGER.error("Failed to load bucket " + p, e);
                }
            }
            return map;
        });
    }

    private void saveBucket(String ctxPath) {
        Map<String, Integer> bucket = bucketsByCtx.get(ctxPath);
        if (bucket == null) return;
        Path file = bucketFile(ctxPath);
        try {
            Files.createDirectories(file.getParent());
            if (bucket.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(new HashMap<>(bucket), w);
            }
        } catch (Exception e) {
            com.xaerocustomcolors.XaeroCustomColors.LOGGER.error("Failed to save bucket " + ctxPath, e);
        }
    }

    private Path bucketFile(String ctxPath) {
        Path target = FabricLoader.getInstance().getGameDir().resolve(ROOT_DIR);
        for (String seg : ctxPath.split("/")) {
            if (seg.isEmpty()) continue;
            target = target.resolve(sanitize(seg));
        }
        return target.resolve(COLOR_FILE);
    }

    private Path legacyFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(LEGACY_FILE);
    }

    private static String sanitize(String s) {
        String r = s.replaceAll("[<>:\"\\\\|?*]", "_");
        if (r.equals(".") || r.equals("..")) r = "_";
        return r.isEmpty() ? "_" : r;
    }
}
