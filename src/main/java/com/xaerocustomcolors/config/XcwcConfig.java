package com.xaerocustomcolors.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.xaerocustomcolors.XaeroCustomColors.LOGGER;

public class XcwcConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "xaerocustomcolors.json";

    private static XcwcConfig instance;

    public Float pickerScale;

    public static XcwcConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static XcwcConfig load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE);
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                XcwcConfig loaded = GSON.fromJson(r, XcwcConfig.class);
                if (loaded != null) {
                    Float s = loaded.pickerScale;
                    if (s != null && !(s > 0f && s <= 1f)) loaded.pickerScale = null;
                    return loaded;
                }
            } catch (Exception e) {
                LOGGER.error("[XCWC] Failed to load config", e);
            }
        }
        return new XcwcConfig();
    }

    public void save() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(FILE + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp)) {
                GSON.toJson(this, w);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOGGER.error("[XCWC] Failed to save config", e);
        }
    }
}
