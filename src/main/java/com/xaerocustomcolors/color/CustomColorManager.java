package com.xaerocustomcolors.color;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.ARGB;
import org.apache.commons.io.file.PathUtils;
import xaero.common.minimap.waypoints.Waypoint;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static com.xaerocustomcolors.XaeroCustomColors.LOGGER;

public class CustomColorManager {

    public static final CustomColorManager INSTANCE = new CustomColorManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final String ROOT_DIR = "xaero_custom_waypoint_colors";
    private static final String COLOR_FILE = "colors.json";
    private static final Pattern SANITIZE = Pattern.compile("[<>:\"\\\\|?*]");

    private final ConcurrentMap<String, ConcurrentMap<String, Integer>> bucketsByCtx = new ConcurrentHashMap<>();
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
        return bucket.get(key);
    }

    public boolean setCustomColor(String ctxPath, Waypoint wp, int argbColor) {
        if (ctxPath == null || wp == null) return false;
        Map<String, Integer> bucket = loadBucket(ctxPath);
        String key = wpKey(wp);
        int color = ARGB.opaque(argbColor);
        Integer old = bucket.get(key);
        if (old != null && old.intValue() == color) return false;
        bucket.put(key, color);
        version.incrementAndGet();
        saveBucket(ctxPath);
        return true;
    }

    public boolean removeCustomColor(String ctxPath, Waypoint wp) {
        return wp != null && removeByKey(ctxPath, wpKey(wp));
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

    public void deleteContainer(String containerNode) {
        if (containerNode == null || containerNode.isEmpty()) return;
        Path root = FabricLoader.getInstance().getGameDir().resolve(ROOT_DIR).normalize();
        Path target = bucketDir(containerNode).normalize();
        if (!root.equals(target.getParent())) return;

        String prefix = containerNode + "/";
        if (bucketsByCtx.keySet().removeIf(ctx -> ctx.equals(containerNode) || ctx.startsWith(prefix))) {
            version.incrementAndGet();
        }

        if (!Files.isDirectory(target)) return;
        try {
            PathUtils.deleteDirectory(target);
            LOGGER.info("[XCWC] Deleted world folder for ({})", containerNode);
        } catch (IOException e) {
            LOGGER.error("[XCWC] Failed to delete custom color folder " + target, e);
        }
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
                    LOGGER.error("[XCWC] Failed to load bucket " + p, e);
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
            Path tmp = file.resolveSibling(COLOR_FILE + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp)) {
                GSON.toJson(new HashMap<>(bucket), w);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOGGER.error("[XCWC] Failed to save bucket " + ctxPath, e);
        }
    }

    private Path bucketDir(String ctxPath) {
        Path target = FabricLoader.getInstance().getGameDir().resolve(ROOT_DIR);
        for (String seg : ctxPath.split("/")) {
            if (seg.isEmpty()) continue;
            target = target.resolve(sanitize(seg));
        }
        return target;
    }

    private Path bucketFile(String ctxPath) {
        return bucketDir(ctxPath).resolve(COLOR_FILE);
    }

    private static String sanitize(String s) {
        String r = SANITIZE.matcher(s).replaceAll("_");
        if (r.equals(".") || r.equals("..")) r = "_";
        return r.isEmpty() ? "_" : r;
    }
}
