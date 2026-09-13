package com.nextos.launcher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent launcher state. Kept in Java for a small, allocation-light prefs layer.
 */
public final class LauncherPreferences {

    public static final String PREF = "next_os_16";
    public static final int DARK_SYSTEM = 0;
    public static final int DARK_LIGHT = 1;
    public static final int DARK_DARK = 2;

    public static final int WALL_AURORA = 0;
    public static final int WALL_DUSK = 1;
    public static final int WALL_SAND = 2;
    public static final int WALL_CUSTOM = 3;

    private static final String K_DARK = "dark_mode";
    private static final String K_GLASS = "liquid_glass";
    private static final String K_SPRING = "spring";
    private static final String K_PARALLAX = "parallax";
    private static final String K_ISLAND = "island";
    private static final String K_LABELS = "labels";
    private static final String K_LOCK_INT = "lock_integrated";
    private static final String K_COLS = "grid_cols";
    private static final String K_ROWS = "grid_rows";
    private static final String K_WALL = "wallpaper";
    private static final String K_WALL_PATH = "wallpaper_path";
    private static final String K_LAYOUT = "home_layout";
    private static final String K_DOCK = "dock";
    private static final String K_HIDDEN = "hidden";
    private static final String K_NAMES = "custom_names";
    private static final String K_PLATES = "custom_plates";
    private static final String K_FOLDERS = "folders";
    private static final String K_WIDGETS = "widgets";
    private static final String K_FIRST = "first_run";

    private final SharedPreferences sp;

    public LauncherPreferences(Context context) {
        this.sp = context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public int darkMode() {
        return sp.getInt(K_DARK, DARK_SYSTEM);
    }

    public void setDarkMode(int mode) {
        sp.edit().putInt(K_DARK, mode).apply();
    }

    public boolean liquidGlass() {
        return sp.getBoolean(K_GLASS, true);
    }

    public void setLiquidGlass(boolean v) {
        sp.edit().putBoolean(K_GLASS, v).apply();
    }

    public boolean spring() {
        return sp.getBoolean(K_SPRING, true);
    }

    public void setSpring(boolean v) {
        sp.edit().putBoolean(K_SPRING, v).apply();
    }

    public boolean parallax() {
        return sp.getBoolean(K_PARALLAX, true);
    }

    public void setParallax(boolean v) {
        sp.edit().putBoolean(K_PARALLAX, v).apply();
    }

    public boolean island() {
        return sp.getBoolean(K_ISLAND, true);
    }

    public void setIsland(boolean v) {
        sp.edit().putBoolean(K_ISLAND, v).apply();
    }

    public boolean labels() {
        return sp.getBoolean(K_LABELS, true);
    }

    public void setLabels(boolean v) {
        sp.edit().putBoolean(K_LABELS, v).apply();
    }

    public boolean lockIntegrated() {
        return sp.getBoolean(K_LOCK_INT, true);
    }

    public void setLockIntegrated(boolean v) {
        sp.edit().putBoolean(K_LOCK_INT, v).apply();
    }

    public int columns() {
        return sp.getInt(K_COLS, 4);
    }

    public int rows() {
        return sp.getInt(K_ROWS, 6);
    }

    public void setGrid(int cols, int rows) {
        sp.edit().putInt(K_COLS, cols).putInt(K_ROWS, rows).apply();
    }

    public int wallpaperId() {
        return sp.getInt(K_WALL, WALL_AURORA);
    }

    public void setWallpaperId(int id) {
        sp.edit().putInt(K_WALL, id).apply();
    }

    public String wallpaperPath() {
        return sp.getString(K_WALL_PATH, null);
    }

    public void setWallpaperPath(String path) {
        sp.edit().putString(K_WALL_PATH, path).apply();
    }

    public boolean isFirstRun() {
        return sp.getBoolean(K_FIRST, true);
    }

    public void setFirstRunDone() {
        sp.edit().putBoolean(K_FIRST, false).apply();
    }

    public List<String> dockPackages() {
        return split(sp.getString(K_DOCK, ""));
    }

    public void setDockPackages(List<String> packages) {
        sp.edit().putString(K_DOCK, TextUtils.join(",", packages)).apply();
    }

    public Set<String> hiddenPackages() {
        return new HashSet<>(split(sp.getString(K_HIDDEN, "")));
    }

    public void setHiddenPackages(Set<String> packages) {
        sp.edit().putString(K_HIDDEN, TextUtils.join(",", packages)).apply();
    }

    public void hide(String pkg, boolean hide) {
        Set<String> set = hiddenPackages();
        if (hide) {
            set.add(pkg);
        } else {
            set.remove(pkg);
        }
        setHiddenPackages(set);
    }

    public Map<String, String> customNames() {
        return readMap(K_NAMES);
    }

    public void setCustomName(String pkg, String name) {
        Map<String, String> map = customNames();
        if (name == null || name.trim().isEmpty()) {
            map.remove(pkg);
        } else {
            map.put(pkg, name.trim());
        }
        writeMap(K_NAMES, map);
    }

    public Map<String, Integer> customPlates() {
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, String> e : readMap(K_PLATES).entrySet()) {
            try {
                out.put(e.getKey(), Integer.parseInt(e.getValue()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public void setCustomPlate(String pkg, int plateRes) {
        Map<String, String> map = readMap(K_PLATES);
        if (plateRes == 0) {
            map.remove(pkg);
        } else {
            map.put(pkg, String.valueOf(plateRes));
        }
        writeMap(K_PLATES, map);
    }

    public JSONObject foldersJson() {
        try {
            return new JSONObject(sp.getString(K_FOLDERS, "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public void setFoldersJson(JSONObject obj) {
        sp.edit().putString(K_FOLDERS, obj.toString()).apply();
    }

    public JSONArray widgetsJson() {
        try {
            return new JSONArray(sp.getString(K_WIDGETS, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public void setWidgetsJson(JSONArray arr) {
        sp.edit().putString(K_WIDGETS, arr.toString()).apply();
    }

    public JSONArray homeLayout() {
        try {
            return new JSONArray(sp.getString(K_LAYOUT, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public void setHomeLayout(JSONArray arr) {
        sp.edit().putString(K_LAYOUT, arr.toString()).apply();
    }

    private Map<String, String> readMap(String key) {
        Map<String, String> map = new HashMap<>();
        try {
            JSONObject o = new JSONObject(sp.getString(key, "{}"));
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                map.put(k, o.optString(k));
            }
        } catch (JSONException ignored) {
        }
        return map;
    }

    private void writeMap(String key, Map<String, String> map) {
        JSONObject o = new JSONObject();
        try {
            for (Map.Entry<String, String> e : map.entrySet()) {
                o.put(e.getKey(), e.getValue());
            }
        } catch (JSONException ignored) {
        }
        sp.edit().putString(key, o.toString()).apply();
    }

    private static List<String> split(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return list;
        }
        for (String p : raw.split(",")) {
            if (!p.isEmpty()) {
                list.add(p);
            }
        }
        return list;
    }
}
